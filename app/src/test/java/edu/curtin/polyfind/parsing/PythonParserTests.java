package edu.curtin.polyfind.parsing;
import edu.curtin.polyfind.definitions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;

import org.apache.commons.io.IOUtils;
import java.io.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.*;
import java.util.stream.*;


class PythonParserTests
{
    private <E> Set<E> set(Stream<E> stream)
    {
        return new HashSet<>(stream.toList());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "@dec1", "@dec2(abc)", "@dec1\n@dec2(abc)", "@dec1(abc)\n@dec2" })
    void decorators(String modifiers)
    {
        var sourceFile = new SourceFile(
            "test_data.py",
            String.format(
                "%s\nclass TestType: pass\n%s\ndef testMethod(): pass\n",
                modifiers, modifiers));

        new PythonParser().parse(sourceFile);

        for(var def : sourceFile.walk().filter(d -> d != sourceFile).toList())
        {
            assertThat(set(def.getModifiers().map(Modifier::toString)))
                .containsOnly((modifiers + "\n").split("\n"));
        }
    }

    @Test
    void knownDecorators()
    {
        var sourceFile = new SourceFile(
            "test_data.py",
            "@classmethod\ndef method1(): pass\n"
            + "@staticmethod\ndef method2(): pass\n"
            + "@abstractmethod\ndef method3(): pass\n"
            + "@abc.abstractmethod\ndef method4(): pass\n"
            + "@classmethod\n@abstractmethod\ndef method5(): pass\n");

        new PythonParser().parse(sourceFile);

        assertThat(sourceFile.walk(MethodDefinition.class))
            .map(d -> set(d.getModifiers()))
            .containsExactly(
                Set.of(Modifier.CLASS_METHOD),
                Set.of(Modifier.STATIC),
                Set.of(Modifier.ABSTRACT),
                Set.of(Modifier.ABSTRACT),
                Set.of(Modifier.ABSTRACT, Modifier.CLASS_METHOD)
            );
    }

    @Test
    void methodParameters()
    {
        var sourceFile = new SourceFile(
            "test_data.py",
            "def testMethod(a, bc: de, fg = lambda hi, jk = lm: no, pq: rs[tu] = 'vw'): pass");

        new PythonParser().parse(sourceFile);

        assertThat(sourceFile.walk(MethodDefinition.class).toList().get(0).getParameters())
            .extracting("name", "type", "defaultValue")
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
            "test_data.py",
            String.format("def testMethod(x, y) -> %s: pass", returnType));

        new PythonParser().parse(sourceFile);

        assertThat(sourceFile.walk(MethodDefinition.class).toList().get(0).getReturnType())
            .isEqualTo(Optional.of(returnType));
    }

    @Test
    void inheritance() throws IOException
    {
        var code =
            "class TestClassA: pass\n"
            + "class TestClassB(metaclass=ABCMeta, TestClassA): pass\n"
            + "class TestClassC(TestInterfaceX): pass\n"
            + "class TestClassD(ABC, TestClassB, TestInterfaceX, TestInterfaceY): pass\n";

        var sourceFile = new SourceFile("test_data.py", code);

        new PythonParser().parse(sourceFile);
        var typeDefs = sourceFile.walk(TypeDefinition.class).toList();

        assertThat(typeDefs)
            .extracting("name", "construct", "metaType", "superTypeSet")
            .containsOnly(
                tuple("TestClassA", "class", Optional.empty(),       Set.of()),
                tuple("TestClassB", "class", Optional.of("ABCMeta"), Set.of("TestClassA")),
                tuple("TestClassC", "class", Optional.empty(),       Set.of("TestInterfaceX")),
                tuple("TestClassD", "class", Optional.empty(),       Set.of("TestClassB", "TestInterfaceX", "TestInterfaceY"))
            );

        assertThat(set(typeDefs.get(1).getModifiers()))
            .containsExactly(Modifier.ABSTRACT);

        assertThat(set(typeDefs.get(3).getModifiers()))
            .containsExactly(Modifier.ABSTRACT);
    }

    @Test
    void nesting()
    {
        var sourceFile = new SourceFile(
            "test_data.py",
            "class A:\n"
            + "  def m1(): pass\n"
            + "  def m2():\n"
            + "    class B: pass\n"
            + "    def m3(x: int) -> int:\n"
            + "      pass\n"
            + "  class C(): pass\n    \n"
            + "class D:\n   pass\n");

        new PythonParser().parse(sourceFile);

        assertThat(sourceFile.getNested())
            .extracting("class", "name")
            .containsExactly(
                tuple(TypeDefinition.class, "A"),
                tuple(TypeDefinition.class, "D")
            );

        assertThat(sourceFile.getNested().toList().get(0).getNested())
            .extracting("class", "name")
            .containsExactly(
                tuple(MethodDefinition.class, "m1"),
                tuple(MethodDefinition.class, "m2"),
                tuple(TypeDefinition.class, "C")
            );

        assertThat(sourceFile.getNested().toList().get(0).getNested().toList().get(1).getNested())
            .extracting("class", "name")
            .containsExactly(
                tuple(TypeDefinition.class, "B"),
                tuple(MethodDefinition.class, "m3")
            );
    }
}

