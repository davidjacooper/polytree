package edu.curtin.polyfind.parsing;
import static edu.curtin.polyfind.parsing.TestUtil.*;
import edu.curtin.polyfind.definitions.*;
import edu.curtin.polyfind.languages.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.*;

import java.io.*;
import java.nio.file.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.*;
import java.util.stream.*;


class JavaParserTests
{
    private static final Path FILE = Path.of("TestData.java");

    private Project project = new Project("test_java_project",
                                          new LanguageSet().getByExtension("java").get());


    static Stream<List<String>> longThings()
    {
        return Stream.of(
            List.of("multi-line comments (/*...*/)", "class A{}/*" + ".".repeat(5000) + "*/"),
            List.of("single-line comments (//...)",  "class A{}//" + ".".repeat(5000)),
            List.of("strings (\"...\")",             "class A{}\"" + ".".repeat(5000) + "\""),
            List.of("generics (<...>)",              "class A<" + ".".repeat(5000) + ">{}"),
            List.of("record header ((...))",         "class A(" + ".".repeat(5000) + "){}"),
            List.of("scope ({...}",                  "class A{" + ".".repeat(5000) + "}"),
            List.of("nested scope ({...}", "class A{{{{{{{{{{" + ".".repeat(5000) + "}}}}}}}}}}")
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
            .isThrownBy(() -> new JavaParser().parse(project, sourceFile));

        assertThat(project.walk())
            .filteredOn("name", "A")
            .singleElement();
    }


    @ParameterizedTest
    @ValueSource(strings = {"class", "interface", "enum", "record"})
    void typeConstructs(String construct)
    {
        var sourceFile = new SourceFile(
            project, FILE,
            String.format(
                "%s TestType%s {}",
                construct,
                "record".equals(construct) ? "(int x, String y)" : ""
            ));

        new JavaParser().parse(project, sourceFile);

        assertThat(project.walk(TypeDefinition.class).toList())
            .extracting(
                TypeDefinition::getName,
                TypeDefinition::getConstruct,
                TypeDefinition::getMetaType,
                d -> sset(d.getSuperTypes()))
            .containsOnly(
                tuple("TestType", construct, Optional.empty(), Set.of())
            );
    }

    @Test
    void anonClass()
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "Object o = new TestType(42, \"abc\") {};");

        new JavaParser().parse(project, sourceFile);

        assertThat(project.walk(TypeDefinition.class))
            .map(d -> sset(d.getSuperTypes()))
            .containsExactly(Set.of("TestType"));
    }


    @ParameterizedTest
    @ValueSource(strings = {"<T>", "<x extends y<z>, w>", "<A<B<C<D<E,F>,G>,H>,I>,J<K<L>>>"})
    void typeParameters(String typeParams)
    {
        var sourceFile = new SourceFile(
            project, FILE,
            String.format("class%s TestType {} %s void testMethod() {}", typeParams, typeParams));

        new JavaParser().parse(project, sourceFile);

        assertThat(project.walk())
            .filteredOn("name", in("TestType", "testMethod"))
            .allSatisfy(d -> assertThat(d.getTypeParams())
                .isEqualTo(Optional.of(typeParams)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abstract", "@Annotation1", "abstract @Annotation1 static",
                            "@Annotation1(abc) final @Annotation2(xyz)"})
    void modifiers(String modifiers)
    {
        var sourceFile = new SourceFile(
            project, FILE,
            String.format("%s class TestType {} %s void testMethod() {}", modifiers, modifiers));

        new JavaParser().parse(project, sourceFile);

        assertThat(project.walk())
            .filteredOn("name", in("TestType", "testMethod"))
            .allSatisfy(d -> assertThat(d.getModifiers().map(Modifier::toString))
                .containsOnly((modifiers + " ").split(" ")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "void", "boolean", "char", "byte", "short", "int", "long", "float", "double",
        "boolean[]", "char[]", "byte[]", "short[]", "int[]", "long[]", "float[]", "double[]",
        "List<@Annot Map<String[],String @Annot[]>[][]>@Annot[]@Annot[][]"
    })
    void methodReturnTypes(String returnType)
    {
        var sourceFile = new SourceFile(
            project, FILE,
            String.format(
                "public %s testMethod1(int x) {} %s testMethod2() {}",
                returnType, returnType));

        new JavaParser().parse(project, sourceFile);

        for(var def : project.walk(MethodDefinition.class).toList())
        {
            assertThat(def.getReturnType().map(Object::toString)).isEqualTo(Optional.of(returnType));
        }
    }

    @Test
    void methodParameters()
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "void testMethod1(X y, @Ann(123) final int[] _abc, List<@Ann X>[]@Ann[] _xyz) {}");

        new JavaParser().parse(project, sourceFile);

        assertThat(project.walk(MethodDefinition.class).toList().get(0).getParameters())
            .map(d -> sset(d.getModifiers()),
                 d -> d.getType().map(Object::toString),
                 ParameterDefinition::getName)
            .containsExactly(
                tuple(Set.of(),                     Optional.of("X"),                    "y"),
                tuple(Set.of("@Ann(123)", "final"), Optional.of("int[]"),                "_abc"),
                tuple(Set.of(),                     Optional.of("List<@Ann X>[]@Ann[]"), "_xyz")
            );
    }

    @Test
    void methodThrows()
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "void testMethod1(int x) throws X, Y<Z> {}");

        new JavaParser().parse(project, sourceFile);

        assertThat(
            set(
                project.walk(MethodDefinition.class).toList().get(0)
                    .getCheckedExceptions()
                    .map(Object::toString)
            )
        )
            .containsOnly("X", "Y<Z>");
    }


    @Test
    void inheritance()
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "package com.example.test;"
            + "public class TestClassA {}"
            + "abstract class TestClassB extends TestClassA {} "
            + "final class TestClassC implements TestInterfaceX {}"
            + "abstract class TestClassD extends TestClassB implements TestInterfaceX, TestInterfaceY {}"
            + "interface TestInterfaceX {}"
            + "interface TestInterfaceY {}"
            + "interface TestInterfaceZ extends TestInterfaceX, TestInterfaceY {}");

        new JavaParser().parse(project, sourceFile);
        var typeDefs = project.walk(TypeDefinition.class).toList();

        assertThat(typeDefs)
            .map(TypeDefinition::getName,
                 TypeDefinition::getConstruct,
                 d -> set(d.getSuperTypes().map(Object::toString)))
            .containsOnly(
                tuple("TestClassA",     "class",     Set.of()),
                tuple("TestClassB",     "class",     Set.of("TestClassA")),
                tuple("TestClassC",     "class",     Set.of("TestInterfaceX")),
                tuple("TestClassD",     "class",     Set.of("TestClassB", "TestInterfaceX", "TestInterfaceY")),
                tuple("TestInterfaceX", "interface", Set.of()),
                tuple("TestInterfaceY", "interface", Set.of()),
                tuple("TestInterfaceZ", "interface", Set.of("TestInterfaceX", "TestInterfaceY"))
            );
    }

    @Test
    void nesting()
    {
        var sourceFile = new SourceFile(
            project, FILE,
            "class A { void m1(); void m2() { class B {} int m3(int x) {} } class C {}}");

        new JavaParser().parse(project, sourceFile);

        var scope = project.getNested().findFirst().get();

        assertThat(scope.getNested())
            .extracting("class", "name")
            .containsOnly(
                tuple(TypeDefinition.class, "A")
            );

        assertThat(scope.getNested().findFirst().get().getNested())
            .extracting("class", "name")
            .containsOnly(
                tuple(MethodDefinition.class, "m1"),
                tuple(MethodDefinition.class, "m2"),
                tuple(TypeDefinition.class, "C")
            );

        assertThat(scope.getNested()
            .findFirst().get().getNested()
            .filter(d -> d.getName().equals("m2")).findFirst().get().getNested()
        )
            .extracting("class", "name")
            .containsOnly(
                tuple(TypeDefinition.class, "B"),
                tuple(MethodDefinition.class, "m3")
            );
    }

    @Test
    void filesAndPackages()
    {
        var sourceFiles = List.of(
            new SourceFile(
                project, Path.of("ClassA.java"),
                "package xx.yy.zz; public class A {}"
            ),

            new SourceFile(
                project, Path.of("ClassB.java"),
                "package xx.yy.zz; class B extends A {}"
            ), // Fine -- superclass 'A' is in the same package

            new SourceFile(
                project, Path.of("ClassC.java"),
                "package xx.yy.ww; class C extends xx.yy.zz.A {}"
            ), // Fine -- superclass 'A' is fully-qualified

            new SourceFile(
                project, Path.of("ClassD.java"),
                "package xx.yy.ww; class D extends A {}"
            ), // Unresolvable -- superclass 'A' is not imported

            new SourceFile(
                project, Path.of("ClassE.java"),
                "package xx.yy.ww; import xx.yy.zz.A; class E extends A {}"
            ), // Fine -- superclass 'A' imported explicitly

            new SourceFile(
                project, Path.of("ClassF.java"),
                "package xx.yy.ww; import xx.yy.zz.*; class F extends A {}"
            ), // Fine -- superclass 'A' imported implicitly

            new SourceFile(
                project, Path.of("ClassG.java"),
                "package xx.yy.ww; import xx.yy.zz.*; class G extends B {}"
            )  // Unresolvable -- superclass 'B' is not public
        );

        var parser = new JavaParser();
        for(var file : sourceFiles)
        {
            parser.parse(project, file);
        }

        var classes = new HashMap<String,TypeDefinition>();
        project.walk(TypeDefinition.class).forEach(d -> classes.put(d.getName(), d));

        for(var name : List.of("B", "C", "E", "F"))
        {
            assertThat(classes.get(name).getSuperTypes())
                .singleElement()
                .extracting(QualifiedTypeName::resolve)
                .as("Supertype of class %s", name)
                .isEqualTo(Optional.of(classes.get("A")));
        }

        for(var name : List.of("D", "G"))
        {
            assertThat(classes.get(name).getSuperTypes())
                .singleElement()
                .extracting(QualifiedTypeName::resolve)
                .as("Unresolvable supertype of class %s", name)
                .isEqualTo(Optional.empty());
        }
    }
}

