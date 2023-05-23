package edu.curtin.polyfind.definitions;

import java.util.*;

public class Modifier
{
    private static final Map<String,Modifier> mods = new HashMap<>();

    public static Modifier ABSTRACT        = named("abstract");
    public static Modifier CLASS_METHOD    = named("classmethod");
    public static Modifier FINAL           = named("final");
    public static Modifier PACKAGE_PRIVATE = named("package-private");
    public static Modifier PRIVATE         = named("private");
    public static Modifier PROTECTED       = named("protected");
    public static Modifier PUBLIC          = named("public");
    public static Modifier STATIC          = named("static");

    public static Modifier named(String s)
    {
        return mods.computeIfAbsent(s, Modifier::new);
    }

    private String name;
    private Modifier(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
