package edu.curtin.polyfind.definitions;

import java.util.*;

public class TypeDefinition 
{    
    private final SourceFile file;
    private final String construct;
    private final String name;
    private final String typeParams;
    private final String superClass;
    private final List<String> interfaces;
    private final List<MethodDefinition> methods;
    
    TypeDefinition(SourceFile file, String construct, String name, String typeParams, String superClass, List<String> interfaces, List<MethodDefinition> methods) 
    {
        this.file = file;
        this.construct = construct;
        this.name = name;
        this.typeParams = typeParams;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.methods = methods;
    }
    
    public SourceFile getSourceFile()           { return file; }
    public boolean isClass()                    { return construct.equals("class"); }
    public String getConstruct()                { return construct; }
    public String getName()                     { return name; }
    public String getTypeParams()               { return typeParams; }
    public String getSuperClass()               { return superClass; }
    public List<String> getInterfaces()         { return interfaces; }
    public List<MethodDefinition> getMethods()  { return methods; }
    
    private String asRawClass(String cls)
    {
        int typeArgIndex = cls.indexOf('<');
        if(typeArgIndex == -1) 
        { 
            return cls; 
        }
        else
        {
            return cls.substring(0, typeArgIndex).trim();
        }
    }
    
    public String getRawSuperClass() 
    { 
        if(superClass == null) { return null; }
        return asRawClass(superClass);
    }
    
    public List<String> getRawInterfaces()
    {
        return interfaces.stream().map(this::asRawClass).toList();
    }
}
