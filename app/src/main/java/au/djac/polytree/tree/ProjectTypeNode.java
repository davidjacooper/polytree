package au.djac.polytree.tree;
import au.djac.polytree.definitions.*;

import java.util.*;

public class ProjectTypeNode extends TypeNode
{
    private final TypeDefinition defn;

    public ProjectTypeNode(TypeDefinition defn)
    {
        this.defn = defn;
    }

    @Override
    public TypeCategory getCategory()
    {
        return defn.getCategory();
    }

    @Override
    public String getConstruct()
    {
        return defn.getConstruct();
    }

    @Override
    public Optional<SourceFile> getSourceFile()
    {
        return defn.getSourceFile();
    }

    @Override
    public String getName()
    {
        return defn.getName();
    }

    @Override
    public Optional<TypeDefinition> getDefinition()
    {
        return Optional.of(defn);
    }

    @Override
    public String toString()
    {
        return
            defn.getConstruct()
            + " "
            + defn.getName()
            + " ("
            + defn.getSourceFile().map(f -> f.getPath().toString()).orElse("no source")
            + ")";
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof ProjectTypeNode && defn.equals(((ProjectTypeNode)other).defn);
    }

    @Override
    public int hashCode()
    {
        return defn.hashCode();
    }
}
