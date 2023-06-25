package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

public class AnonymousScope extends ScopedDefinition
{
    private static int sequenceNumber = 0;

    private static String placeholderName()
    {
        sequenceNumber++;
        return "-anonymous-" + sequenceNumber + "-";
    }

    public AnonymousScope(SourceFile file, int startPos, int endPos)
    {
        super(file, startPos, endPos, placeholderName());
    }

    public AnonymousScope()
    {
        super(placeholderName());
    }

    @Override
    public Stream<ScopedDefinition> getNamedScopes()
    {
        return getNested().flatMap(ScopedDefinition::getNamedScopes);
    }

    @Override
    public <D extends ScopedDefinition> Stream<D> resolveHere(Class<D> defnClass,
                                                              List<String> names)
    {
        return getNested().flatMap(d -> d.resolveHere(defnClass, names));
    }

    @Override
    public String toString()
    {
        return "`[scope]`";
    }
}
