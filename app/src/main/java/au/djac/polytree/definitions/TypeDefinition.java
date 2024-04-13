package au.djac.polytree.definitions;

import java.util.*;
import java.util.stream.*;

public class TypeDefinition extends ScopedDefinition
{
    private final String construct;
    private final TypeCategory category;
    private Optional<QualifiedTypeName> metaType = Optional.empty();
    private final Set<QualifiedTypeName> superTypes = new HashSet<>();

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

    public TypeCategory getCategory()             { return category; }

    public String getConstruct()                  { return construct; }
    public Stream<QualifiedTypeName> getSuperTypes()  { return superTypes.stream(); }
    public Optional<QualifiedTypeName> getMetaType()  { return metaType; }

    public Stream<MethodDefinition> getMethods()
    {
        return getNested()
            .filter(d -> d instanceof MethodDefinition)
            .map(d -> (MethodDefinition)d);
    }

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
