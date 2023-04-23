package edu.curtin.polyfind.tree;

import java.util.*;

public class Signature
{
    private final String name;
    private final List<TypeNode> parameters;
    
    public Signature(String name, List<TypeNode> parameters)
    {
        this.name = name;
        this.parameters = parameters;
    }
    
    public String getName() { return name; }
    public List<TypeNode> getParameters() { return parameters; }
    
    @Override
    public boolean equals(Object other)
    {
        if(!(other instanceof Signature)) { return false; }
        
        var otherSig = (Signature)other;
        return name.equals(otherSig.name) && parameters.equals(otherSig.parameters);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(name, parameters);
    }
}
