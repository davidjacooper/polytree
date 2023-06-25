package edu.curtin.polyfind.tree;
import edu.curtin.polyfind.definitions.*;

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

    // @Override
    // public boolean isClass()
    // {
    //     return defn.isClass();
    // }

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


// package edu.curtin.polyfind.tree;
// import edu.curtin.polyfind.definitions.*;
//
// import java.util.*;
//
// public class TypeNode implements Comparable<TypeNode>
// {
//     // private final Optional<SourceFile> file;
//     private final String name;
//     private final boolean isClass;
//     private final Optional<TypeDefinition> defn;
//     private final List<TypeNode> parents = new ArrayList<>();
//     private final List<TypeNode> children = new ArrayList<>();
//     private final List<MethodNode> methods = new ArrayList<>();
//
//     public TypeNode(TypeDefinition defn)
//     {
//         this.defn = Optional.of(defn);
//         // this.file = defn.getSourceFile();
//         this.name = defn.getName();
//         this.isClass = defn.isClass();
//     }
//
//     public TypeNode(String name, boolean isClass)
//     {
//         // this.file = Optional.empty();
//         this.defn = Optional.empty();
//         this.name = name;
//         this.isClass = isClass;
//     }
//
//     public void addParent(TypeNode parent)
//     {
//         parents.add(parent);
//     }
//
//     public void addChild(TypeNode child)
//     {
//         children.add(child);
//     }
//
//     public void addMethod(MethodNode method)
//     {
//         methods.add(method);
//     }
//
//     public Optional<SourceFile> getSourceFile()
//     {
//         return defn.flatMap(TypeDefinition::getSourceFile);
//     }
//
//     public String getName() { return name; }
//     public boolean isClass() { return isClass; }
//     public Optional<TypeDefinition> getDefinition() { return defn; }
//     public List<TypeNode> getParents() { return Collections.unmodifiableList(parents); }
//     public List<TypeNode> getChildren() { return Collections.unmodifiableList(children); }
//     public List<MethodNode> getMethods() { return Collections.unmodifiableList(methods); }
//
//     @Override
//     public String toString()
//     {
//         // return (isClass ? "class " : "interface ") + name + " (" + file.map(f -> f.getName()).orElse("no source") + ")";
//         return
//             defn.map(d -> d.getConstruct()).orElseGet(() -> isClass ? "class " : "interface ")
//             + name
//             + " ("
//             + getSourceFile().map(f -> f.getPath().toString()).orElse("no source")
//             + ")";
//     }
//
//     @Override
//     public int compareTo(TypeNode other)
//     {
//         return name.compareTo(other.name);
//     }
//
//     // @Override
//     // public boolean equals(Object other)
//     // {
//     //     if(!(other instanceof TypeNode)) { return false; }
//     //     var otherType = (TypeNode)other;
//     //     return name.equals(otherType.name) &&
//     //         // ((file == null) ? (otherType.file == null) : Objects.equals(file.getPackage(), otherType.file.getPackage()));
//     //         getFile().map(SourceFile::getPackage).equals(
//     //             otherType.getFile().map(SourceFile::getPackage));
//     // }
//
//     @Override
//     public boolean equals(Object other)
//     {
//         if(!(other instanceof TypeNode)) { return false; }
//         var otherType = (TypeNode)other;
//         return name.equals(otherType.name) && defn.equals(otherType.defn);
//     }
//
//     @Override
//     public int hashCode()
//     {
//         // return Objects.hash(name, (file == null) ? null : file.getPackage());
//         // return Objects.hash(name, file.map(SourceFile::getPackage));
//         return Objects.hash(name, defn);
//     }
// }
