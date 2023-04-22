package edu.curtin.polyfind.tree;
import edu.curtin.polyfind.definitions.*;

import java.util.*;

public class TypeNode implements Comparable<TypeNode>
{
    private final SourceFile file;
    private final String name;
    private final boolean isClass;
    private final TypeDefinition defn;
    private final List<TypeNode> parents = new ArrayList<>();
    private final List<TypeNode> children = new ArrayList<>();
    private final List<MethodNode> methods = new ArrayList<>();
    
    public TypeNode(TypeDefinition defn)
    {
        this.defn = defn;
        this.file = defn.getSourceFile();
        this.name = defn.getName();
        this.isClass = defn.isClass();
    }
    
    public TypeNode(String name, boolean isClass)
    {
        this.file = null;
        this.defn = null;
        this.name = name;
        this.isClass = isClass;
    }
    
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

    public SourceFile getSourceFile() { return file; }
    public String getName() { return name; }
    public boolean isClass() { return isClass; }
    public TypeDefinition getDefinition() { return defn; }
    public List<TypeNode> getParents() { return Collections.unmodifiableList(parents); }
    public List<TypeNode> getChildren() { return Collections.unmodifiableList(children); }
    public List<MethodNode> getMethods() { return Collections.unmodifiableList(methods); }
    
    @Override
    public String toString()
    {
        return (isClass ? "class " : "interface ") + name + " (" + ((file == null) ? "no source" : file.getName()) + ")";
    }
    
    @Override
    public int compareTo(TypeNode other)
    {
        return name.compareTo(other.name);
    }
    
    @Override
    public boolean equals(Object other)
    {
        if(!(other instanceof TypeNode)) { return false; }
        var otherType = (TypeNode)other;
        return name.equals(otherType.name) && 
            ((file == null) ? (otherType.file == null) : Objects.equals(file.getPackage(), otherType.file.getPackage()));
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(name, (file == null) ? null : file.getPackage());
    }
}
