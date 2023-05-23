package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public abstract class ScopedDefinition extends Definition
{
    private Optional<ScopedDefinition> containing = Optional.empty();
    private Optional<String> typeParams           = Optional.empty();
    private final List<ScopedDefinition> nested = new ArrayList<>();

    public ScopedDefinition(SourceFile file, int startPos, int endPos, String name)
    {
        super(file, startPos, endPos, name);
    }

    public void setContaining(ScopedDefinition containing)
    {
        this.containing = Optional.of(containing);
    }

    public void setTypeParams(String typeParams)
    {
        this.typeParams = Optional.of(typeParams);
    }

    public void addNested(ScopedDefinition defn)
    {
        if(defn.getStartPos() < getStartPos() || defn.getEndPos() > getEndPos())
        {
            throw new IllegalArgumentException(
                String.format(
                    "Definition %s (%d-%d) cannot be nested within %s (%d-%d)",
                    defn, defn.getStartPos(), defn.getEndPos(),
                    this, getStartPos(), getEndPos()));
        }
        nested.add(defn);
    }

    public Optional<ScopedDefinition> getContaining() { return containing; }
    public Optional<String> getTypeParams()           { return typeParams; }
    public Stream<ScopedDefinition> getNested()       { return nested.stream(); }
    public Stream<ScopedDefinition> walk()
    {
        return Stream.concat(
            Stream.of(this),
            nested.stream().flatMap(ScopedDefinition::walk)
        );
    }

    public <D extends ScopedDefinition> Stream<D> walk(Class<D> defnClass)
    {
        var stream = walk().filter(d -> defnClass.isAssignableFrom(d.getClass()));

        @SuppressWarnings("unchecked")
        var castStream = (Stream<D>)stream;
        return castStream;
    }

    public Optional<String> getPublicName()
    {
        return getPublicScope().map(
            scope -> (scope.isEmpty() ? "" : (scope + ".")) + getName());
    }

    public abstract Optional<String> getPublicScope();
    // public abstract String getDisplayName();
}
