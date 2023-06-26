package edu.curtin.polyfind.parsing;
import static edu.curtin.polyfind.parsing.TestUtil.*;
import edu.curtin.polyfind.definitions.*;
import edu.curtin.polyfind.languages.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.*;
import java.util.stream.*;


class PythonParserTests
{
    private static final Path FILE = Path.of("test_data.py");

    private Project project = new Project("test_python_project",
                                          new LanguageSet().getByExtension("py").get());


    static Stream<List<String>> longThings()
    {
        return Stream.of(
            List.of("comments (#...)",     "class A: pass #" + ".".repeat(5000)),
            List.of("strings (\"...\")",   "class A: pass \"" + ".".repeat(5000) + "\""),
            List.of("parentheses ((...))", "class A(" + "z,".repeat(5000) + "): pass"),
            List.of("parentheses ((...))", "def A(z = z(" + "1".repeat(5000) + ")): pass"),
            List.of("brackets ([...])",    "def A(z = [" + "1".repeat(5000) + "]): pass"),
            List.of("braces ({...})",      "def A(z = {" + "1".repeat(5000) + "}): pass"),
            List.of("scope (\\n...)",      "class A:\n" + "  .\n".repeat(5000) + ".")
        );
    }

    @ParameterizedTest
    @MethodSource("longThings")
    void longThingsTest(List<String> arg)
    {
        var label = arg.get(0);
        var code = arg.get(1);
        var sourceFile = new SourceFile(project, FILE, code);

        assertThatNoException()
            .describedAs("parsing long " + label)
            .isThrownBy(() -> new PythonParser().parse(project, sourceFile));

        assertThat(project.walk())
            .filteredOn("name", "A")
            .singleElement();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "@dec1", "@dec2(abc)", "@dec1\n@dec2(abc)", "@dec1(abc)\n@dec2" })
    void decorators(String modifiers)
    {
        var sourceFile = new SourceFile(
            project, FILE,
            String.format(
                "%s\nclass TestType: pass\n%s\ndef testMethod(): pass\n",
                modifiers, modifiers));

        new PythonParser().parse(project, sourceFile);

        assertThat(project.walk())
            .filteredOn("name", in("TestType", "testMethod"))
            .allSatisfy(d -> assertThat(d.getModifiers().map(Modifier::toString))
                .containsOnly((modifiers + "\n").split("\n")));
    }

    @Test
    void knownDecorators()
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "@classmethod\ndef method1(): pass\n"
            + "@staticmethod\ndef method2(): pass\n"
            + "@abstractmethod\ndef method3(): pass\n"
            + "@abc.abstractmethod\ndef method4(): pass\n"
            + "@classmethod\n@abstractmethod\ndef method5(): pass\n");

        new PythonParser().parse(project, sourceFile);

        assertThat(project.walk(MethodDefinition.class))
            .extracting(
                Definition::getName,
                d -> set(d.getModifiers())
            )
            .containsOnly(
                tuple("method1", Set.of(Modifier.CLASS_METHOD)),
                tuple("method2", Set.of(Modifier.STATIC)),
                tuple("method3", Set.of(Modifier.ABSTRACT)),
                tuple("method4", Set.of(Modifier.ABSTRACT)),
                tuple("method5", Set.of(Modifier.ABSTRACT, Modifier.CLASS_METHOD))
            );
    }

    @Test
    void methodParameters()
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "def testMethod(a, bc: de, fg = lambda hi, jk = lm: no, pq: rs[tu] = 'vw'): pass");

        new PythonParser().parse(project, sourceFile);

        assertThat(project.walk(MethodDefinition.class).toList().get(0).getParameters())
            .extracting(
                ParameterDefinition::getName,
                d -> d.getType().map(Object::toString),
                ParameterDefinition::getDefaultValue
            )
            .containsExactly(
                tuple("a",  Optional.empty(),      Optional.empty()),
                tuple("bc", Optional.of("de"),     Optional.empty()),
                tuple("fg", Optional.empty(),      Optional.of("lambda hi, jk = lm: no")),
                tuple("pq", Optional.of("rs[tu]"), Optional.of("'vw'"))
            );
    }

    @ParameterizedTest
    @ValueSource(strings = { "int", "float", "List[List[str]]" })
    void methodReturnTypes(String returnType)
    {
        var sourceFile = new SourceFile(
            project, FILE,
            String.format("def testMethod(x, y) -> %s: pass", returnType));

        new PythonParser().parse(project, sourceFile);

        assertThat(project.walk(MethodDefinition.class))
            .singleElement()
            .extracting(d -> d.getReturnType().map(Object::toString))
            .isEqualTo(Optional.of(returnType));
    }

    @Test
    void inheritance() throws IOException
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "class TestClassA: pass\n"
            + "class TestClassB(metaclass=ABCMeta, TestClassA): pass\n"
            + "class TestClassC(TestInterfaceX): pass\n"
            + "class TestClassD(ABC, TestClassB, TestInterfaceX, TestInterfaceY): pass\n");

        new PythonParser().parse(project, sourceFile);
        var typeDefs = project.walk(TypeDefinition.class).toList();

        assertThat(typeDefs)
            .extracting(
                Definition::getName,
                TypeDefinition::getConstruct,
                d -> d.getMetaType().map(Object::toString),
                d -> sset(d.getSuperTypes())
            )
            .containsOnly(
                tuple("TestClassA", "class", Optional.empty(),       Set.of()),
                tuple("TestClassB", "class", Optional.of("ABCMeta"), Set.of("TestClassA")),
                tuple("TestClassC", "class", Optional.empty(),       Set.of("TestInterfaceX")),
                tuple("TestClassD", "class", Optional.empty(),       Set.of("TestClassB", "TestInterfaceX", "TestInterfaceY"))
            );

        assertThat(typeDefs)
            .filteredOn("name", in("TestClassB", "TestClassD"))
            .allSatisfy(d -> assertThat(set(d.getModifiers()))
                .isEqualTo(Set.of(Modifier.ABSTRACT)));
    }

    @Test
    void nesting()
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "class A:\n"
            + "  def m1(): pass\n"
            + "  def m2():\n"
            + "    class B: pass\n"
            + "    def m3(x: int) -> int:\n"
            + "      pass\n"
            + "  class C(): pass\n    \n"
            + "class D:\n   pass\n");

        new PythonParser().parse(project, sourceFile);

        assertThat(project.getNested()).satisfiesExactly(
            module -> {
                assertThat(module)
                    .extracting("class", "name", "construct")
                    .containsExactly(PackageDefinition.class, "test_data", "module");
                assertThat(module.getNested()).satisfiesExactly(
                    classA -> {
                        assertThat(classA)
                            .extracting("class", "name")
                            .containsExactly(TypeDefinition.class, "A");
                        assertThat(classA.getNested()).satisfiesExactlyInAnyOrder(
                            m1 -> assertThat(m1)
                                .extracting("class", "name")
                                .containsExactly(MethodDefinition.class, "m1"),
                            m2 -> {
                                assertThat(m2)
                                    .extracting("class", "name")
                                    .containsExactly(MethodDefinition.class, "m2");
                                assertThat(m2.getNested()).satisfiesExactly(
                                    classB -> assertThat(classB)
                                        .extracting("class", "name")
                                        .containsExactly(TypeDefinition.class, "B"),
                                    m3 -> assertThat(m3)
                                        .extracting("class", "name")
                                        .containsExactly(MethodDefinition.class, "m3")
                                );
                            },
                            classC -> assertThat(classC)
                                .extracting("class", "name")
                                .containsExactly(TypeDefinition.class, "C")
                        );
                    },
                    classD -> assertThat(classD)
                        .extracting("class", "name")
                        .containsExactly(TypeDefinition.class, "D")
                );
            });
    }
}

