package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public class Import
{
    public interface Supplier
    {
        Stream<Import> get();
    }

    private final ScopedDefinition source;
    private final List<String> localName;

    public Import(ScopedDefinition source, List<String> localName)
    {
        this.source = source;
        this.localName = Collections.unmodifiableList(new ArrayList<>(localName));
    }

    public Import(ScopedDefinition source, String name)
    {
        this.source = source;
        this.localName = List.of(name);
    }

    public ScopedDefinition getSource() { return source; }
    public List<String> getLocalName()  { return localName; }

    @Override
    public String toString()
    {
        return String.format("`import %s as %s`", source, localName);
    }
}


// package edu.curtin.polyfind.definitions;
//
// public class Import
// {
//     private ScopedDefinition source;
//     private QualifiedName localName;
//
//     public Import(QualifiedName source, QualifiedName localName)
//     {
//         this.source = source;
//         this.localName = localName;
//     }
//
//     public ScopedDefinition getSource() { return source; }
//     public QualifiedName getLocalName() { return localName; }
// }
//


// package edu.curtin.polyfind.definitions;
//
// public class Import
// {
//     private ScopedDefinition source;
//     private QualifiedName localName;
//
//     public Import(QualifiedName source, QualifiedName localName)
//     {
//         this.source = source;
//         this.localName = localName;
//     }
//
//     public ScopedDefinition getSource() { return source; }
//     public QualifiedName getLocalName() { return localName; }
// }



// package edu.curtin.polyfind.definitions;
//
// public class Import
// {
//     private ScopedDefinition baseScope;
//     private QualifiedName source;
//     private Optional<QualifiedName> localName;
//
//     public Import(ScopedDefinition baseScope, QualifiedName source, QualifiedName localName)
//     {
//         this.baseScope = baseScope;
//         this.source = source;
//         this.localName = Optional.of(localName);
//     }
//
//     public Import(ScopedDefinition baseScope, QualifiedName source)
//     {
//         this.baseScope = baseScope;
//         this.source = source;
//         this.localName = Optional.empty();
//     }
//
//     public ScopedDefinition getBaseScope()        { return baseScope; }
//     public QualifiedName getSource()              { return source; }
//     public Optional<QualifiedName> getLocalName() { return localName; }
// }
