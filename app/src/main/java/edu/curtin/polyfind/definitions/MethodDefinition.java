package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.regex.*;

public class MethodDefinition 
{
    private Set<String> modifiers;
    private String typeParams;
    private String returnType;
    private String name;
    private List<ParameterDefinition> parameters;
    
    public MethodDefinition(Set<String> modifiers, String typeParams, String returnType, String name, List<ParameterDefinition> parameters) 
    {
        this.modifiers = modifiers;
        this.typeParams = typeParams;
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
    }
    
    public Set<String> getModifiers()                { return modifiers; }
    public String getTypeParams()                    { return typeParams; }
    public String getReturnType()                    { return returnType; }
    public String getName()                          { return name; }    
    public List<ParameterDefinition> getParameters() { return parameters; }
    
    @Override
    public String toString()
    {
        return "[" + String.join(" ", modifiers) + "]" + ((typeParams == null) ? "" : typeParams) + " " + returnType + " " + name + "(" + 
            String.join(",", parameters.stream().map(ParameterDefinition::getType).toList()) + ")";
    }
}
