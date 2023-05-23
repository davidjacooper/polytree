package edu.curtin.polyfind.tree;
import edu.curtin.polyfind.definitions.*;

import java.util.*;

public class ParameterNode implements Comparable<ParameterNode>
{
    private final ParameterDefinition defn;
    private final MethodNode method;
    private Optional<TypeNode> type = Optional.empty();
    private final String name;

    public ParameterNode(ParameterDefinition defn, MethodNode method)
    {
        this.defn = defn;
        this.method = method;
        this.name = defn.getName();
    }

    public void setType(TypeNode type)
    {
        this.type = Optional.of(type);
    }

    public ParameterDefinition getDefinition() { return defn; }
    public MethodNode getMethod()              { return method; }
    public Optional<TypeNode> getType()        { return type; }
    public String getName()                    { return name; }

    @Override
    public int compareTo(ParameterNode other)
    {
        var e1 = type.isEmpty();
        var e2 = other.type.isEmpty();
        if(e1 && e2) { return 0; }
        if(e1)       { return -1; }
        if(e2)       { return 1; }
        return type.get().compareTo(other.type.get());
    }

    @Override
    public boolean equals(Object other)
    {
        if(!(other instanceof ParameterNode)) { return false; }
        return type.equals(((ParameterNode)other).type);
    }

    @Override
    public int hashCode()
    {
        return type.hashCode();
    }
}
