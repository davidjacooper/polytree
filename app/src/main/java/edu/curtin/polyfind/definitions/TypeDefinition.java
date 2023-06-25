package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public class TypeDefinition extends ScopedDefinition
{
    private final String construct;
    private final TypeCategory category;
    private Optional<QualifiedTypeName> metaType = Optional.empty();
    private final Set<QualifiedTypeName> superTypes = new HashSet<>();

    // public TypeDefinition(SourceFile file, int startPos, int endPos, String name, String construct)
    // {
    //     this(file, startPos, endPos, name, ScopeType.NAMESPACE, construct);
    // }
    //
    // public TypeDefinition(SourceFile file, int startPos, int endPos,
    //                       String name, ScopeType scopeType, String construct)
    // {
    //     super(file, startPos, endPos, name, scopeType);
    //     this.construct = construct;
    // }

    public TypeDefinition(SourceFile file, int startPos, int endPos,
                          String name, TypeCategory category, String construct)
    {
        super(file, startPos, endPos, name);
        this.category = category;
        this.construct = construct;
    }

    public QualifiedTypeName addSuperType(List<String> names, String displayName)
    {
        var qName = new QualifiedTypeName(this, names, displayName, false);
        this.superTypes.add(qName);
        return qName;
    }

    public QualifiedTypeName setMetaType(List<String> names, String displayName)
    {
        var qName = new QualifiedTypeName(this, names, displayName, false);
        this.metaType = Optional.of(qName);
        return qName;
    }

    // public boolean isClass()                      { return construct.equals("class"); }
    public TypeCategory getCategory()             { return category; }

    public String getConstruct()                  { return construct; }
    public Stream<QualifiedTypeName> getSuperTypes()  { return superTypes.stream(); }
    // public List<QualifiedName> getSuperTypeList() { return superTypes; }
    public Optional<QualifiedTypeName> getMetaType()  { return metaType; }

    public Stream<MethodDefinition> getMethods()
    {
        return getNested()
            .filter(d -> d instanceof MethodDefinition)
            .map(d -> (MethodDefinition)d);
    }

    // private String asRawClass(String cls)
    // {
    //     int typeArgIndex = cls.indexOf('<');
    //     if(typeArgIndex == -1)
    //     {
    //         return cls;
    //     }
    //     else
    //     {
    //         return cls.substring(0, typeArgIndex).trim();
    //     }
    // }
    //
    // public Stream<String> getRawSuperTypes()
    // {
    //     return superTypes.stream().map(this::asRawClass);
    // }

    // @Override
    // public Optional<String> getPublicScope()
    // {
    //     var containing = getContaining();
    //     if(containing.isEmpty()) { return Optional.of(getName()); }
    //
    //     return containing.flatMap(c -> c.getPublicScope()).map(s -> s + "." + getName());
    // }

    @Override
    public String toString()
    {
        var mods = getModifiersString().strip();

        return "`"
            + mods + (mods.isEmpty() ? "" : " ")
            + construct + " "
            + getName()
            + getTypeParams().orElse("")
            + (superTypes.isEmpty()
                ? ""
                : ("(" + superTypes.stream().map(Object::toString)
                                            .collect(Collectors.joining(",")) + ")"))
            + "`";
    }
}
