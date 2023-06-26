package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public class Import
{
    public interface Supplier
    {
        Stream<Import> get();
    }

    private final ScopedDefinition source;
    private final List<String> localName;

    public Import(ScopedDefinition source, List<String> localName)
    {
        this.source = source;
        this.localName = Collections.unmodifiableList(new ArrayList<>(localName));
    }

    public Import(ScopedDefinition source, String name)
    {
        this.source = source;
        this.localName = List.of(name);
    }

    public ScopedDefinition getSource() { return source; }
    public List<String> getLocalName()  { return localName; }

    @Override
    public String toString()
    {
        return String.format("`import %s as %s`", source, localName);
    }
}
