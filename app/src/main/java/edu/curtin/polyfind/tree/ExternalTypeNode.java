package edu.curtin.polyfind.tree;
import edu.curtin.polyfind.definitions.*;

import java.util.*;

public class ExternalTypeNode extends TypeNode
{
    private final String name;
    private final TypeCategory category;
    private final String construct;

    public ExternalTypeNode(String name, TypeCategory category, String construct)
    {
        this.name = name;
        this.category = category;
        this.construct = construct;
    }

    @Override
    public TypeCategory getCategory()
    {
        return category;
    }

    @Override
    public String getConstruct()
    {
        return construct;
    }

    @Override
    public Optional<SourceFile> getSourceFile()
    {
        return Optional.empty();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Optional<TypeDefinition> getDefinition()
    {
        return Optional.empty();
    }

    @Override
    public String toString()
    {
        return construct + " " + name + " (external)";
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof ExternalTypeNode && name.equals(((ExternalTypeNode)other).name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
}
