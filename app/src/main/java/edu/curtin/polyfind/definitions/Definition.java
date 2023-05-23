package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public abstract class Definition
{
    private final SourceFile file;
    private final int startPos;
    private final int endPos;
    private final String name;
    private Set<Modifier> modifiers = new HashSet<>();

    public Definition(SourceFile file, int startPos, int endPos, String name)
    {
        this.file = file;
        this.startPos = startPos;
        this.endPos = endPos;
        this.name = name;
    }

    public void addModifier(Modifier modifier)
    {
        modifiers.add(modifier);
    }

    public SourceFile getSourceFile()      { return file; }
    public int getStartPos()               { return startPos; }
    public int getEndPos()                 { return endPos; }
    public String getName()                { return name; }
    public Stream<Modifier> getModifiers() { return modifiers.stream(); }
    public Set<Modifier> getModifierSet()  { return Collections.unmodifiableSet(modifiers); }
    public boolean is(Modifier mod)        { return modifiers.contains(mod); }

    public String getModifiersString()
    {
        return modifiers.stream().map(Modifier::toString).collect(Collectors.joining(" "));
    }
}
