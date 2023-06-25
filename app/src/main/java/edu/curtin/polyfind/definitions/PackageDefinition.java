package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public class PackageDefinition extends ScopedDefinition
{
    private final String construct;

    public PackageDefinition(SourceFile file, int startPos, int endPos,
                             String name, String construct)
    {
        super(file, startPos, endPos, name);
        // setAscendable(false);
        // setDescendable(true);
        this.construct = construct;
    }

    public PackageDefinition(String name, String construct)
    {
        super(name);
        // setAscendable(false);
        // setDescendable(true);
        this.construct = construct;
    }

    public String getConstruct() { return construct; }

    // @Override
    // public Optional<String> getPublicScope() { ... }

    @Override
    public String toString()
    {
        return String.format("`%s %s`", construct, getName());
    }
}
