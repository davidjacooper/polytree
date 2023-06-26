package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public class MethodDefinition extends ScopedDefinition
{
    private Optional<QualifiedTypeName> returnType = Optional.empty();
    private boolean isConstructor = false;
    private final List<ParameterDefinition> parameters = new ArrayList<>();
    private final Set<QualifiedTypeName> checkedExceptions = new HashSet<>();

    public MethodDefinition(SourceFile file, int startPos, int endPos, String name)
    {
        super(file, startPos, endPos, name);
        setDescendable(false);
    }

    public void addParameter(ParameterDefinition param)
    {
        parameters.add(param);
    }

    public QualifiedTypeName addCheckedException(List<String> names, String displayName)
    {
        var qName = new QualifiedTypeName(this, names, displayName, false);
        checkedExceptions.add(qName);
        return qName;
    }

    public QualifiedTypeName setReturnType(List<String> names, String displayName)
    {
        var qName = new QualifiedTypeName(this, names, displayName, false);
        this.returnType = Optional.of(qName);
        return qName;
    }

    public void setConstructor(boolean isConstructor)
    {
        this.isConstructor = isConstructor;
    }

    public Optional<QualifiedTypeName> getReturnType()      { return returnType; }
    public boolean isConstructor()                          { return isConstructor; }
    public Stream<ParameterDefinition> getParameters()      { return parameters.stream(); }
    public Stream<QualifiedTypeName> getCheckedExceptions() { return checkedExceptions.stream(); }

    @Override
    public String toString()
    {
        var mods = getModifiersString().strip();
        var typeParams = getTypeParams().orElse("").strip();

        return "`"
            + mods + (mods.isEmpty() ? "" : " ")
            + typeParams + (typeParams.isEmpty() ? "" : " ")
            + returnType.map(QualifiedName::toString).orElse("?") + " "
            + getName() + "("
            + parameters.stream()
                        .map(p -> p.getType().map(QualifiedName::toString).orElse(p.getName()))
                        .collect(Collectors.joining(",")) + ")"
            + "`";
    }
}
