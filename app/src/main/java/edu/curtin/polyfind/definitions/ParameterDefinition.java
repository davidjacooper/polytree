package edu.curtin.polyfind.definitions;

import java.util.*;

public class ParameterDefinition 
{
    private Set<String> modifiers;
    private String type;
    private String name;
    
    public ParameterDefinition(Set<String> modifiers, String type, String name) 
    {
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
    }
    
    public Set<String> getModifiers() { return modifiers; }
    public String getType() { return type; }
    public String getName() { return name; }    
    
    @Override
    public String toString()
    {
        return "[" + String.join(" ", modifiers) + "]" + " " + type + " " + name;
    }
}
