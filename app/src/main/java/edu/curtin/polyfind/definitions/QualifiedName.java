package edu.curtin.polyfind.definitions;

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

    // public <D extends Definition> Stream<D> resolve(Class<D> defnClass)
    // {
    //     return resolve(withinScope ? scope : scope.getContaining().get(),
    //                    defnClass);
    // }
    //
    // private <D extends Definition> Stream<D> resolve(ScopedDefinition scope,
    //                                                  Class<D> defnClass)
    // {
    //     System.out.printf("resolve(%s,%s)\n", scope, defnClass);
    //     return Stream.<Supplier<Stream<D>>>of(
    //         () -> resolveLocally(scope, defnClass, names),
    //         () -> resolveFromImports(scope, defnClass, names),
    //         () ->
    //             scope.isAscendable()
    //                 ? scope.getContaining().stream().flatMap(superScope -> resolve(superScope, defnClass))
    //                 : Stream.empty())
    //         .flatMap(Supplier::get);
    // }
    //
    // // public <D extends Definition> Stream<D> resolveFromImports(Class<D> defnClass)
    // // {
    // //     return resolveFromImports(scope, defnClass, names);
    // // }
    //
    // private static <D extends Definition> Stream<D> resolveFromImports(ScopedDefinition scope,
    //                                                                    Class<D> defnClass,
    //                                                                    List<String> names)
    // {
    //     System.out.printf("resolveFromImports(%s,%s,%s)\n", scope, defnClass, names);
    //     System.out.println(scope.getImports().toList());
    //
    //     return scope.getImports().flatMap(import_ ->
    //     {
    //         var importNames = import_.getLocalName();
    //         var importNameSize = importNames.size();
    //         var thisNameSize = names.size();
    //
    //         System.out.printf("  trying import %s\n", importNames);
    //
    //         if(!(importNameSize <= thisNameSize &&
    //             names.subList(0, importNameSize).equals(importNames)))
    //         {
    //             return Stream.empty();
    //         }
    //
    //         var importedDefn = import_.getSource();
    //
    //         if(importNameSize == thisNameSize)
    //         {
    //             if(defnClass.isAssignableFrom(importedDefn.getClass()))
    //             {
    //                 @SuppressWarnings("unchecked")
    //                 var importedDefnD = (D) importedDefn;
    //
    //                 return Stream.of(importedDefnD);
    //             }
    //             else
    //             {
    //                 return Stream.empty();
    //             }
    //         }
    //
    //         return resolveLocally(importedDefn,
    //                               defnClass,
    //                               names.subList(importNameSize, thisNameSize));
    //      });
    // }
    //
    // // public <D extends Definition> Stream<D> resolveLocally(Class<D> defnClass)
    // // {
    // //     return resolveLocally(scope, defnClass, names);
    // // }
    //
    // public static <D extends Definition> Stream<D> resolveLocally(ScopedDefinition scope,
    //                                                               Class<D> defnClass,
    //                                                               List<String> names)
    // {
    //     System.out.printf("resolveLocally(%s,%s,%s)\n", scope, defnClass, names);
    //     var size = names.size();
    //     if(size == 0) { throw new IllegalArgumentException("Name list cannot be empty"); }
    //
    //     var topName = names.get(0);
    //     if(size == 1)
    //     {
    //         return scope
    //             .getNested()
    //             .filter(subScope -> topName.equals(subScope.getName()) &&
    //                                 defnClass.isAssignableFrom(subScope.getClass()))
    //             .map(subScope ->
    //             {
    //                 @SuppressWarnings("unchecked")
    //                 var d = (D)subScope;
    //                 return d;
    //             });
    //     }
    //
    //     return scope
    //         .getNested()
    //         .filter(subScope -> topName.equals(subScope.getName()))
    //         .flatMap(subScope ->
    //             subScope.isDescendable()
    //                 ? resolveLocally(subScope, defnClass, names.subList(1, size))
    //                 : Stream.empty()
    //         );
    // }

}

// package edu.curtin.polyfind.definitions;
//
// import java.util.*;
//
// public final class QualifiedName extends LinkedList<String>
// {
//     public static QualifiedName of(String name)
//     {
//         var qn = new QualifiedName();
//         for(var part : name.split("\\."))
//         {
//             var part_ = part.strip();
//             if(part_.isEmpty())
//             {
//                 throw new IllegalArgumentException("'" + name + "'contains empty name component(s)");
//             }
//             qn.add(part.strip());
//         }
//         return qn;
//     }
//
//     @Override
//     public String toString()
//     {
//         return String.join(".", this);
//     }
//
//     public boolean startsWith(QualifiedName other)
//     {
//         var otherSize = other.size();
//         return otherSize <= size() && subList(0, otherSize).equals(other);
//     }
//
//     public boolean equalSize(QualifiedName other)
//     {
//         return size() == other.size();
//     }
// }
