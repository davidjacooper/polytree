package au.djac.polytree.parsing;
import au.djac.polytree.definitions.*;

import java.util.*;
import java.util.stream.*;

final class TestUtil
{
    static <E> Set<E> set(Stream<E> stream)
    {
        return new TreeSet<>(stream.toList());
    }

    static Set<String> sset(Stream<?> stream)
    {
        return new TreeSet<>(stream.map(Object::toString).toList());
    }

    static void dumpDefnTree(ScopedDefinition defn)
    {
        dumpDefnTree(defn, "");
    }

    static void dumpDefnTree(ScopedDefinition defn, String indent)
    {
        System.out.print(indent);
        System.out.println(defn);
        defn.getNested().forEach(subDefn -> dumpDefnTree(subDefn, indent + "  "));
    }
}
