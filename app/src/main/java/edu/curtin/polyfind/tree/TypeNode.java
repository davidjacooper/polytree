package edu.curtin.polyfind.tree;
import edu.curtin.polyfind.definitions.*;

import java.util.*;

public abstract class TypeNode implements Comparable<TypeNode>
{
    private final List<TypeNode> parents = new ArrayList<>();
    private final List<TypeNode> children = new ArrayList<>();
    private final List<MethodNode> methods = new ArrayList<>();

    public void addParent(TypeNode parent)
    {
        parents.add(parent);
    }

    public void addChild(TypeNode child)
    {
        children.add(child);
    }

    public void addMethod(MethodNode method)
    {
        methods.add(method);
    }

    public abstract Optional<SourceFile> getSourceFile();
    public abstract String getName();
    public abstract Optional<TypeDefinition> getDefinition();
    public abstract TypeCategory getCategory();
    public abstract String getConstruct();

    public List<TypeNode> getParents() { return Collections.unmodifiableList(parents); }
    public List<TypeNode> getChildren() { return Collections.unmodifiableList(children); }
    public List<MethodNode> getMethods() { return Collections.unmodifiableList(methods); }

    @Override
    public int compareTo(TypeNode other)
    {
        return getName().compareTo(other.getName());
    }

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
