package edu.curtin.polyfind.tree;
import edu.curtin.polyfind.definitions.*;

import java.util.*;

public class ParameterNode implements Comparable<ParameterNode>
{
    private final ParameterDefinition defn;
    private final MethodNode method;
    private final TypeNode type;
    private final String name;
    
    public ParameterNode(ParameterDefinition defn, MethodNode method, TypeNode type) 
    {
        this.defn = defn;
        this.method = method;
        this.type = type;
        this.name = defn.getName();
    }
    
    public ParameterDefinition getDefinition() { return defn; }    
    public MethodNode getMethod()              { return method; }
    public TypeNode getType()                  { return type; }    
    public String getName()                    { return name; }
    
    @Override
    public int compareTo(ParameterNode other)
    {
        return type.compareTo(other.type);
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
