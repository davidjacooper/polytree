package edu.curtin.polyfind.parsing;
import edu.curtin.polyfind.definitions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.*;

import java.io.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.*;
import java.util.stream.*;


class JavaParserTests
{
    private <E> Set<E> set(Stream<E> stream)
    {
        return new TreeSet<>(stream.toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"class", "interface", "enum", "record"})
    void typeConstructs(String construct)
    {
        var sourceFile = new SourceFile(
            "TestData.java",
            String.format(
                "%s TestType%s {}",
                construct,
                "record".equals(construct) ? "(int x, String y)" : ""
            ));

        new JavaParser().parse(sourceFile);

        assertThat(sourceFile.walk(TypeDefinition.class).toList())
            .extracting("name", "construct", "metaType", "superTypeSet")
            .containsOnly(
                tuple("TestType", construct, Optional.empty(), Set.of())
            );
    }

    @Test
    void anonClass()
    {
        var sourceFile = new SourceFile(
            "TestData.java",
            "Object o = new TestType(42, \"abc\") {};");

        new JavaParser().parse(sourceFile);

        assertThat(sourceFile.walk(TypeDefinition.class))
            .map(d -> set(d.getSuperTypes()))
            .containsExactly(Set.of("TestType"));
    }


    @ParameterizedTest
    @ValueSource(strings = {"<T>", "<x extends y<z>, w>", "<A<B<C<D<E,F>,G>,H>,I>,J<K<L>>>"})
    void typeParameters(String typeParams)
    {
        var sourceFile = new SourceFile(
            "TestData.java",
            String.format("class%s TestType {} %s void testMethod() {}", typeParams, typeParams));

        new JavaParser().parse(sourceFile);

        for(var def : sourceFile.walk().filter(d -> d != sourceFile).toList())
        {
            assertThat(def.getTypeParams()).isEqualTo(Optional.of(typeParams));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abstract", "@Annotation1", "abstract @Annotation1 static",
                            "@Annotation1(abc) final @Annotation2(xyz)"})
    void modifiers(String modifiers)
    {
        var sourceFile = new SourceFile(
            "TestData.java",
            String.format("%s class TestType {} %s void testMethod() {}", modifiers, modifiers));

        new JavaParser().parse(sourceFile);

        for(var def : sourceFile.walk().filter(d -> d != sourceFile).toList())
        {
            assertThat(set(def.getModifiers().map(Modifier::toString)))
                .containsOnly((modifiers + " ").split(" "));
        }
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
            "TestData.java",
            String.format(
                "public %s testMethod1(int x) {} %s testMethod2() {}",
                returnType, returnType));

        new JavaParser().parse(sourceFile);

        for(var def : sourceFile.walk(MethodDefinition.class).toList())
        {
            assertThat(def.getReturnType()).isEqualTo(Optional.of(returnType));
        }
    }

    @Test
    void methodParameters()
    {
        var sourceFile = new SourceFile(
            "TestData.java",
            "void testMethod1(X y, @Ann(123) final int[] _abc, List<@Ann X>[]@Ann[] _xyz) {}");

        new JavaParser().parse(sourceFile);

        assertThat(sourceFile.walk(MethodDefinition.class).toList().get(0).getParameters())
            .map(d -> set(d.getModifiers().map(Modifier::toString)),
                 ParameterDefinition::getType,
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
            "TestData.java",
            "void testMethod1(int x) throws X, Y<Z> {}");

        new JavaParser().parse(sourceFile);

        assertThat(set(sourceFile.walk(MethodDefinition.class).toList().get(0).getCheckedExceptions()))
            .containsOnly("X", "Y<Z>");
    }


    @Test
    void inheritance()
    {
        var code =
            "package com.example.test;"
            + "public class TestClassA {}"
            + "abstract class TestClassB extends TestClassA {} "
            + "final class TestClassC implements TestInterfaceX {}"
            + "abstract class TestClassD extends TestClassB implements TestInterfaceX, TestInterfaceY {}"
            + "interface TestInterfaceX {}"
            + "interface TestInterfaceY {}"
            + "interface TestInterfaceZ extends TestInterfaceX, TestInterfaceY {}";

        var sourceFile = new SourceFile(
            "TestData.java",
            code);

        new JavaParser().parse(sourceFile);
        var typeDefs = sourceFile.walk(TypeDefinition.class).toList();

        assertThat(typeDefs)
            .extracting("name",         "construct", "superTypeSet")
            .containsOnly(
                tuple("TestClassA",     "class",     Set.of()),
                tuple("TestClassB",     "class",     Set.of("TestClassA")),
                tuple("TestClassC",     "class",     Set.of("TestInterfaceX")),
                tuple("TestClassD",     "class",     Set.of("TestClassB", "TestInterfaceX", "TestInterfaceY")),
                tuple("TestInterfaceX", "interface", Set.of()),
                tuple("TestInterfaceY", "interface", Set.of()),
                tuple("TestInterfaceZ", "interface", Set.of("TestInterfaceX", "TestInterfaceY"))
            );

        assertThat(typeDefs)
            .extracting("sourceFile.package")
            .containsOnly(Optional.of("com.example.test"));
    }

    @Test
    void nesting()
    {
        var sourceFile = new SourceFile(
            "TestData.java",
            "class A { void m1(); void m2() { class B {} int m3(int x) {} } class C {}}");

        new JavaParser().parse(sourceFile);

        assertThat(sourceFile.getNested())
            .extracting("class", "name")
            .containsExactly(
                tuple(TypeDefinition.class, "A")
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

