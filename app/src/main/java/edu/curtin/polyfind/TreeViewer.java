package edu.curtin.polyfind;
import edu.curtin.polyfind.tree.*;

import org.fusesource.jansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import static java.lang.Math.max;
import java.io.*;
import java.util.*;

public class TreeViewer 
{
    private static final String DEFAULT = "";
    private static final String BRIGHT_WHITE = "1";
    private static final String GREY = "30;1";
    private static final String RED = "31";
    private static final String GREEN = "32";
    private static final String ORANGE = "33";
    private static final String MAGENTA = "35";
    private static final String CYAN = "36";
    private static final String BRIGHT_MAGENTA = "35;1";

    static {
        AnsiConsole.systemInstall();
    }
    
    private int column = 0;
    private int terminalWidth;
    private PrintStream out;
    
    public static TreeViewer withAnsi()
    {
        var out = AnsiConsole.out();
        out.setMode(AnsiMode.Force);
        return new TreeViewer(out);
    }
        
    public TreeViewer(PrintStream out)
    {
        terminalWidth = AnsiConsole.getTerminalWidth();
        if(terminalWidth < 1)
        {
            terminalWidth = 80;
        }
        this.out = out;
    }

    public void view(Collection<TypeNode> types) 
    {    
        var typeList = new ArrayList<>(types);
        typeList.sort(null);
            
        var rootTypes           = typeList.stream().filter(t -> t.getParents().isEmpty() && !t.getChildren().isEmpty()).toList();
        var nonInheritanceTypes = typeList.stream().filter(t -> t.getParents().isEmpty() && t.getChildren().isEmpty() && t.getSourceFile() != null).toList(); 
            
        if(rootTypes.isEmpty())
        {
            println("No inheritance found", BRIGHT_MAGENTA);
        }
        else
        {
            println("Inheritance Tree(s)", BRIGHT_MAGENTA);
            println("(Note: subclasses will appear multiple times if using multiple inheritance.)");
            for(var type : rootTypes)
            {
                printNodeTree("    ", "    ", "    ", type, null);
                newLine();
            }
        }
        
        if(!nonInheritanceTypes.isEmpty())
        {
            println("Types Without Inheritance (no super/subclasses)", BRIGHT_MAGENTA);
            for(var type: nonInheritanceTypes)
            {
                printNodeTree("    ", "    ", "    ", type, null);
                newLine();
            }
        }
    }  
        
    private void printNodeTree(String abovePrefix, String connectingPrefix, String belowPrefix, TypeNode type, TypeNode parent)
    {
        println(abovePrefix);
        
        List<TypeNode> otherParents = new ArrayList<>(type.getParents());
        otherParents.remove(parent);
        if(otherParents.size() > 0)
        {
            otherParents.sort(null);
            println(abovePrefix);
            for(var otherParent : otherParents)
            {
                print(abovePrefix);
                print(otherParent.isClass() ? "class " : "interface ", GREY);
                println(otherParent.getName(), GREY);
            }
            print(abovePrefix);
            println("┊", GREY);
        }
                
        print(connectingPrefix);
        if(type.isClass())
        {
            print("class ", GREEN);
        }
        else
        {
            print("interface ", RED);
        }
        print(type.getName(), BRIGHT_WHITE);
        
        var typeDefn = type.getDefinition();
        if(typeDefn != null)
        {
            var typeParams = typeDefn.getTypeParams();
            if(typeParams != null)
            {
                print(typeParams, GREY);
            }
        }
        
        var sourceFile = type.getSourceFile();
        if(sourceFile == null)
        {   
            println(" [no source]", GREY);
        }
        else
        {
            var packageName = sourceFile.getPackage();
            if(packageName == null)
            {
                newLine();
            }
            else
            {
                printRight("[package " + packageName + "]", GREY);
            }
        }
        
        var children = type.getChildren();
        var nChildren = children.size();
        viewMethods(belowPrefix + ((nChildren > 0) ? "│   " : "    "), type);
        
        int i = 0;
        for(var child : children)
        {
            if(i < nChildren - 1)
            {
                printNodeTree(belowPrefix + "│   ", belowPrefix + "├── ", belowPrefix + "│   ", child, type);
            }
            else
            {
                printNodeTree(belowPrefix + "│   ", belowPrefix + "└── ", belowPrefix + "    ", child, type);
            }
            i++;
        }
    }
    
    private void viewMethods(String prefix, TypeNode type)
    {
        var allMethods = new ArrayList<>(type.getMethods());
        if(allMethods.isEmpty())
        {
            print(prefix);
            if(type.getSourceFile() == null)
            {
                println("[methods unknown]", GREY);
            }
            else
            {
                println("[no methods]", GREY);
            }
        }
        else
        {
            int nPrivateMethods = 0;
            allMethods.sort(null);
            
            var allModifiers = new HashMap<MethodNode,TreeSet<String>>();
            
            int modifierWidth = 0;
            int typeParamWidth = 0;
            int returnTypeWidth = 0;
            int signatureWidth = 0;
            for(var method : allMethods)
            {
                var defn = method.getDefinition();
                var modifiers = new TreeSet<>(defn.getModifiers());
                allModifiers.put(method, modifiers);
                
                if(modifiers.contains("private"))
                {   
                    nPrivateMethods++;
                }
                else
                {
                    if(method.getOverrides() != null)
                    {
                        modifiers.add("@Override");
                    }
                    
                    if(modifiers.contains("public"))
                    {
                        modifiers.remove("public");
                    }
                    else if(type.isClass() && !modifiers.contains("protected"))
                    {
                        modifiers.add("package-private");
                    }
                    
                    if(!type.isClass() && !modifiers.contains("default"))
                    {
                        modifiers.add("abstract");
                    }
                    
                    var typeParams = defn.getTypeParams();
                    var params     = method.getParameters(); 
                    
                    typeParamWidth  = max(typeParamWidth, (typeParams == null) ? 0 : typeParams.length() + 1);
                    modifierWidth   = max(modifierWidth, modifiers.stream().mapToInt(s -> s.length() + 1).sum());
                    returnTypeWidth = max(returnTypeWidth, defn.getReturnType().length() + 1);
                    signatureWidth  = max(signatureWidth, 
                                          defn.getName().length() + 2 + (params.isEmpty() 
                                              ? 2
                                              : params.stream().mapToInt(p -> p.getType().getName().length() + 2).sum()));
                }
            }
        
            boolean first = true;
            for(var method : allMethods)
            {
                var defn = method.getDefinition();
                var modifiers = allModifiers.get(method);
                
                if(!modifiers.contains("private"))
                {
                    if(first)
                    {
                        first = false;
                        println(prefix);
                    }
                    print(prefix);
                             
                    int startCol = column;
                    for(var mod : modifiers)
                    {
                        print(mod, (mod.equals("@Override") || mod.equals("abstract")) ? MAGENTA : ORANGE);
                        print(" ");
                    }
                    print(" ".repeat(modifierWidth - (column - startCol)));
                    
                    startCol = column;
                    var typeParams = defn.getTypeParams();
                    if(typeParams != null)
                    {
                        print(typeParams, GREY);
                    }
                    print(" ".repeat(typeParamWidth - (column - startCol)));
                    
                    startCol = column;
                    print(defn.getReturnType(), GREY);
                    print(" ".repeat(returnTypeWidth - (column - startCol)));
                    
                    startCol = column;
                    print(method.getName());
                    print("(");
                    printJoin(", ", DEFAULT, method.getParameters().stream().map(p -> p.getType().getName()).toList(), GREY);
                    print(")");
                    print(" ".repeat(signatureWidth - (column - startCol)));
                    
                    var overriddenBy = method.getOverriddenBy();
                    if(!overriddenBy.isEmpty())
                    {
                        var overriddingTypeList = new ArrayList<>(overriddenBy.stream().map(m -> m.getType().getName()).toList());
                        overriddingTypeList.sort(null);
                        print("[overridden in ", GREY);
                        printJoin(", ", GREY, overriddingTypeList, CYAN);
                        print("]", GREY);
                    }
                    newLine();
                }
            }
            if(nPrivateMethods > 0)
            {
                print(prefix);
                println("[" + nPrivateMethods + " private method" + ((nPrivateMethods == 1) ? "" : "s") + "]", GREY);
            }
        }
    }
    
    private void print(String s)
    {
        column += s.length();
        out.print(s);
    }
    
    private void print(String s, String colourCode)
    {
        column += s.length();
        out.printf("\033[%sm%s\033[m", colourCode, s);
    }
    
    private void newLine()
    {
        column = 0;
        out.println();
    }
    
    private void println(String s)
    {
        print(s);
        newLine();
    }
    
    private void println(String s, String colourCode)
    {
        print(s, colourCode);
        newLine();
    }
    
    private void printRight(String s, String colourCode)
    {
        out.print(" ".repeat(max(0, terminalWidth - (column + s.length()))));
        println(s, colourCode);
    }
    
    private void printJoin(String separator, String separatorColourCode, List<String> values, String valueColourCode)
    {
        boolean first = true;
        for(var s : values)
        {
            if(!first)
            {
                print(separator, separatorColourCode);
            }
            first = false;
            print(s, valueColourCode);
        }
    }
}
