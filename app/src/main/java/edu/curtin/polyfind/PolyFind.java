package edu.curtin.polyfind;
import edu.curtin.polyfind.definitions.*;
import edu.curtin.polyfind.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PolyFind 
{
    public static void main(String[] args) 
    {
        if(args.length > 1)
        {
            System.err.println("Too many command-line parameters.");
            System.exit(1);
        }
        
        try
        {
            var parser = new JavaParser();
            var treeBuilder = new TreeBuilder();
            for(var file : Files.walk(Path.of((args.length == 0) ? "." : args[0]))
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toString().endsWith(".java"))
                                .toList())
            {               
                var sourceFile = SourceFile.read(file);
                for(var defn : parser.parse(sourceFile))
                {
                    treeBuilder.addDefinition(defn);
                }
            }

            var types = new ArrayList<>(treeBuilder.build());
            types.sort(null);
            
            var rootTypes   = types.stream().filter(t -> t.getParents().isEmpty() && !t.getChildren().isEmpty()).toList();
            var orphanTypes = types.stream().filter(t -> t.getParents().isEmpty() && t.getChildren().isEmpty() && t.getSourceFile() != null).toList(); 
            
            if(!rootTypes.isEmpty())
            {
                System.out.println("\033[35;1mInheritance Tree(s)\033[m");
                System.out.println("(Note: subclasses will appear multiple times if using multiple inheritance.)");
                for(var type : rootTypes)
                {
                    // if(type.getParents().size() == 0 && type.getChildren().size() > 0)
                    {
                        printNodeTree("    ", "    ", "    ", type, null);
                        System.out.println();
                    }
                }
            }
            
            if(!orphanTypes.isEmpty())
            {
                System.out.println("\033[35;1mOrphaned Types (no super/subclasses)\033[m");
                for(var type: orphanTypes)
                {
                    printNodeTree("    ", "    ", "    ", type, null);
                    System.out.println();
                }
            }
        }
        catch(IOException e)
        {
            System.err.println("IO Error: " + e.getMessage());
        }
    }
        
    private static void printNodeTree(String preIndent, String firstLineIndent, String indent, TypeNode node, TypeNode parent)
    {
    
        System.out.println(preIndent);
        
        List<TypeNode> otherParents = new ArrayList<>(node.getParents());
        otherParents.remove(parent);
        if(otherParents.size() > 0)
        {
            otherParents.sort(null);
            System.out.println(preIndent);
            for(var otherParent : otherParents)
            {
                System.out.println(preIndent + "\033[30;1m[" + (otherParent.isClass() ? "class" : "interface") + " " + otherParent.getName() + "]\033[m");
            }
            System.out.println(preIndent + "\033[30;1m┊\033[m");
        }
        
        System.out.print(
            firstLineIndent
            + (node.isClass() ? "\033[32mclass\033[m " : ("\033[31minterface\033[m "))
            + "\033[1m" + node.getName() + "\033[m    "
        );
        
        var sourceFile = node.getSourceFile();
        var children = node.getChildren();
        var nChildren = children.size();
        var methodIndent = indent + ((nChildren > 0) ? "│   " : "   ");       
        
        if(sourceFile == null)
        {   
            System.out.println("\033[30;1m[no source]\033[m");
            System.out.println(methodIndent + "\033[30;1m[methods unknown]\033[m");
        }
        else
        {
            var packageName = sourceFile.getPackage();
            if(packageName != null)
            {
                System.out.print("\033[30;1m[package " + packageName + "]\033[m");
            }
            System.out.println();
                
            var allMethods = new ArrayList<>(node.getMethods());
            if(allMethods.isEmpty())
            {
                System.out.println(methodIndent + "\033[30;1m[no methods]\033[m");
            }
            else
            {
                int nPrivateMethods = 0;
                allMethods.sort(null);
            
                boolean first = true;
                
                for(var method : allMethods)
                {
                    var defn = method.getDefinition();
                    var modifiers = new HashSet<>(defn.getModifiers());
                    
                    if(modifiers.contains("private"))
                    {   
                        nPrivateMethods++;
                    }
                    else
                    {
                        if(first)
                        {
                            first = false;
                            System.out.println(methodIndent);
                        }
                        System.out.print(methodIndent);
                        if(method.getOverrides() != null)
                        {
                            System.out.print("\033[33m@Override\033[m ");
                        }
                        
                        if(modifiers.contains("public"))
                        {
                            modifiers.remove("public");
                        }
                        else if(node.isClass() && !modifiers.contains("protected"))
                        {
                            modifiers.add("package-private");
                        }
                        
                        if(!node.isClass() && !modifiers.contains("default"))
                        {
                            modifiers.add("abstract");
                        }
                        
                        if(!modifiers.isEmpty())
                        {
                            System.out.print("\033[33m" + String.join(" ", modifiers) + "\033[m ");
                        }
                        
                        System.out.print("\033[30;1m" + defn.getReturnType() + "\033[m " + method.getName() + "(" +
                            String.join(", ", method.getParameters().stream().map(p -> "\033[30;1m" + p.getType().getName() + "\033[m").toList()) + ")"
                        );
                        
                        // var overriddenBy = method.getOverriddenBy();
                        // if(!overriddenBy.isEmpty())
                        // {
                        //     var overriddingTypeList = new ArrayList<>(overriddenBy.stream().map(m -> m.getType().getName()).toList());
                        //     overriddingTypeList.sort(null);
                        //     System.out.print(" \033[36m[overridden in " + String.join(", ", overriddingTypeList) + "]\033[m");
                        // }
                        System.out.println();
                    }
                }
                if(nPrivateMethods > 0)
                {
                    System.out.println(methodIndent + "\033[30;1m[" + nPrivateMethods + " private method(s)]\033[m");
                }
            }
        }
        
        int i = 0;
        for(var child : children)
        {
            if(i < nChildren - 1)
            {
                printNodeTree(indent + "│   ", indent + "├── ", indent + "│   ", child, node);
            }
            else
            {
                printNodeTree(indent + "│   ", indent + "└── ", indent + "    ", child, node);
            }
            i++;
        }
    }
}
