package au.djac.polytree.definitions;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

public class QualifiedName
{
    private final ScopedDefinition scope;
    private final List<String> names;
    private final String displayName;
    private final boolean withinScope;

    public QualifiedName(ScopedDefinition scope, List<String> names, String displayName, boolean withinScope)
    {
        this.scope = scope;
        this.names = new ArrayList<>(names);
        this.displayName = displayName;
        this.withinScope = withinScope;
    }

    public List<String> getNames() { return names; }

    @Override
    public boolean equals(Object otherObj)
    {
        if(!(otherObj instanceof QualifiedName)) { return false; }

        var other = (QualifiedName)otherObj;
        return scope.equals(other.scope) && names.equals(other.names);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(scope, names);
    }

    @Override
    public String toString()
    {
        return displayName;
    }

    public <D extends ScopedDefinition> Stream<D> resolve(Class<D> defnClass)
    {
        return (withinScope ? scope : scope.getContaining().get()).resolve(defnClass, names);
    }
}
