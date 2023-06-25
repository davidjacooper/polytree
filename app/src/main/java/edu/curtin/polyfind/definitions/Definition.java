package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.stream.*;

public abstract class Definition
{
    private Optional<SourceFile> file;
    private int startPos;
    private int endPos;
    private final String name;
    private Set<Modifier> modifiers = new HashSet<>();

    public Definition(SourceFile file, int startPos, int endPos, String name)
    {
        this.file = Optional.of(file);
        this.startPos = startPos;
        this.endPos = endPos;
        this.name = name;
    }

    public Definition(String name)
    {
        this.file = Optional.empty();
        this.startPos = -1;
        this.endPos = -1;
        this.name = name;
    }

    public void setLocation(SourceFile file, int startPos, int endPos)
    {
        this.file = Optional.of(file);
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public void addModifier(Modifier modifier)
    {
        modifiers.add(modifier);
    }

    public Optional<SourceFile> getSourceFile() { return file; }
    public int getStartPos()                    { return startPos; }
    public int getEndPos()                      { return endPos; }
    public String getName()                     { return name; }
    public Stream<Modifier> getModifiers()      { return modifiers.stream(); }
    // public Set<Modifier> getModifierSet()       { return Collections.unmodifiableSet(modifiers); }
    // public boolean is(Modifier mod)             { return modifiers.contains(mod); }

//     public boolean hasModifier(Modifier... queryModifiers)
//     {
//         for(var mod : queryModifiers)
//         {
//             if(!this.modifiers.contains(mod))
//             {
//                 return false;
//             }
//         }
//         return true;
//     }
//
//     public boolean hasAnyModifier(Modifier... queryModifiers)
//     {
//         for(var mod : queryModifiers)
//         {
//             if(this.modifiers.contains(mod))
//             {
//                 return true;
//             }
//     }

    public boolean hasModifier(Modifier... queryModifiers)
    {
        return hasModifier(false, queryModifiers);
    }

    public boolean hasAnyModifier(Modifier... queryModifiers)
    {
        return hasModifier(true, queryModifiers);
    }

    private boolean hasModifier(boolean findAny, Modifier... queryModifiers)
    {
        for(var mod : queryModifiers)
        {
            if(this.modifiers.contains(mod) == findAny)
            {
                return findAny;
            }
        }
        return !findAny;
    }


    public String getModifiersString()
    {
        return modifiers.stream().map(Modifier::toString).collect(Collectors.joining(" "));
    }
}

// package edu.curtin.polyfind.definitions;
//
// import java.util.*;
// import java.util.stream.*;
//
// public abstract class Definition
// {
//     private Optional<SourceFile> file;
//     private int startPos;
//     private int endPos;
//     private final String name;
//     private Set<Modifier> modifiers = new HashSet<>();
//
//     public Definition(SourceFile file, int startPos, int endPos, String name)
//     {
//         this.file = Optional.of(file);
//         this.startPos = startPos;
//         this.endPos = endPos;
//         this.name = name;
//     }
//
//     public Definition(String name)
//     {
//         this.file = Optional.empty();
//         this.startPos = -1;
//         this.endPos = -1;
//         this.name = name;
//     }
//
//     public void setLocation(SourceFile file, int startPos, int endPos)
//     {
//         this.file = Optional.of(file);
//         this.startPos = startPos;
//         this.endPos = endPos;
//     }
//
//     public void addModifier(Modifier modifier)
//     {
//         modifiers.add(modifier);
//     }
//
//     public Optional<SourceFile> getSourceFile() { return file; }
//     public int getStartPos()                    { return startPos; }
//     public int getEndPos()                      { return endPos; }
//     public String getName()                     { return name; }
//     public Stream<Modifier> getModifiers()      { return modifiers.stream(); }
//     // public Set<Modifier> getModifierSet()       { return Collections.unmodifiableSet(modifiers); }
//     // public boolean is(Modifier mod)             { return modifiers.contains(mod); }
//
// //     public boolean hasModifier(Modifier... queryModifiers)
// //     {
// //         for(var mod : queryModifiers)
// //         {
// //             if(!this.modifiers.contains(mod))
// //             {
// //                 return false;
// //             }
// //         }
// //         return true;
// //     }
// //
// //     public boolean hasAnyModifier(Modifier... queryModifiers)
// //     {
// //         for(var mod : queryModifiers)
// //         {
// //             if(this.modifiers.contains(mod))
// //             {
// //                 return true;
// //             }
// //     }
//
//     public boolean hasModifier(Modifier... queryModifiers)
//     {
//         return hasModifier(false, queryModifiers);
//     }
//
//     public boolean hasAnyModifier(Modifier... queryModifiers)
//     {
//         return hasModifier(true, queryModifiers);
//     }
//
//     private boolean hasModifier(boolean findAny, Modifier... queryModifiers)
//     {
//         for(var mod : queryModifiers)
//         {
//             if(this.modifiers.contains(mod) == findAny)
//             {
//                 return findAny;
//             }
//         }
//         return !findAny;
//     }
//
//
//     public String getModifiersString()
//     {
//         return modifiers.stream().map(Modifier::toString).collect(Collectors.joining(" "));
//     }
// }
