package au.djac.polytree.definitions;

import java.util.*;

public class ParameterDefinition extends Definition
{
    private Optional<QualifiedTypeName> type = Optional.empty();
    private Optional<String> defaultValue = Optional.empty();
    private boolean implicit = false;

    public ParameterDefinition(SourceFile file, int startPos, int endPos, String name)
    {
        super(file, startPos, endPos, name);
    }

    public void setType(QualifiedTypeName type)
    {
        this.type = Optional.of(type);
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = Optional.of(defaultValue);
    }

    public void setImplicit(boolean implicit)
    {
        this.implicit = implicit;
    }

    public Optional<QualifiedTypeName> getType() { return type; }
    public Optional<String> getDefaultValue()    { return defaultValue; }
    public boolean isImplicit()                  { return implicit; }

    @Override
    public String toString()
    {
        return "[" + getModifiersString() + "] "
            + type.map(t -> t + " ").orElse("")
            + getName();
    }
}
