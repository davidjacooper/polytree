package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public class TypeDefinition extends ScopedDefinition
{
    private final String construct;
    private Optional<String> metaType = Optional.empty();
    private final Set<String> superTypes = new HashSet<>();

    public TypeDefinition(SourceFile file, int startPos, int endPos, String name, String construct)
    {
        super(file, startPos, endPos, name);
        this.construct = construct;
    }

    public void addSuperType(String superType)
    {
        this.superTypes.add(superType);
    }

    public void setMetaType(String metaType)
    {
        this.metaType = Optional.of(metaType);
    }

    public boolean isClass()              { return construct.equals("class"); }
    public String getConstruct()          { return construct; }
    public Stream<String> getSuperTypes() { return superTypes.stream(); }
    public Set<String> getSuperTypeSet()  { return superTypes; }
    public Optional<String> getMetaType() { return metaType; }

    public Stream<MethodDefinition> getMethods()
    {
        return getNested()
            .filter(d -> d instanceof MethodDefinition)
            .map(d -> (MethodDefinition)d);
    }

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

    public Stream<String> getRawSuperTypes()
    {
        return superTypes.stream().map(this::asRawClass);
    }

    @Override
    public Optional<String> getPublicScope()
    {
        var containing = getContaining();
        if(containing.isEmpty()) { return Optional.of(getName()); }

        return containing.flatMap(c -> c.getPublicScope()).map(s -> s + "." + getName());
    }

    @Override
    public String toString()
    {
        return "[" + getModifiersString() + "]"
            + " " + construct
            + getTypeParams().orElse("") + " "
            + getName() + "("
            + String.join(",", superTypes) + ")";
    }
}
