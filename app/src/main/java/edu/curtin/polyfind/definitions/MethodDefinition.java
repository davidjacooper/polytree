package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public class MethodDefinition extends ScopedDefinition
{
    private Optional<String> returnType = Optional.empty();
    private boolean isConstructor = false;
    private final List<ParameterDefinition> parameters = new ArrayList<>();
    private final Set<String> checkedExceptions = new HashSet<>();

    public MethodDefinition(SourceFile file, int startPos, int endPos, String name)
    {
        super(file, startPos, endPos, name);
    }

    public void addParameter(ParameterDefinition param)
    {
        parameters.add(param);
    }

    public void addCheckedException(String exception)
    {
        checkedExceptions.add(exception);
    }

    public void setReturnType(String returnType)
    {
        this.returnType = Optional.of(returnType);
    }

    public void setConstructor(boolean isConstructor)
    {
        this.isConstructor = isConstructor;
    }

    public Optional<String> getReturnType()            { return returnType; }
    public boolean isConstructor()                     { return isConstructor; }
    public Stream<ParameterDefinition> getParameters() { return parameters.stream(); }
    public Stream<String> getCheckedExceptions()       { return checkedExceptions.stream(); }

    @Override
    public String toString()
    {
        return "[" + getModifiersString() + "]"
            + getTypeParams().orElse("") + " "
            + returnType.orElse("?") + " "
            + getName() + "("
            + parameters.stream()
                        .map(p -> p.getType().orElse(p.getName()))
                        .collect(Collectors.joining(",")) + ")";
    }

    @Override
    public Optional<String> getPublicScope()
    {
        return Optional.empty();
    }
}
