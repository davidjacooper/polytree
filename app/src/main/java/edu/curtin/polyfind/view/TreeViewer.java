package edu.curtin.polyfind.view;
import edu.curtin.polyfind.tree.*;
import static edu.curtin.polyfind.view.Output.*;

// import org.fusesource.jansi.*;
// import static org.fusesource.jansi.Ansi.Color.*;

import static java.lang.Math.max;
import java.util.*;

public class TreeViewer
{
//     private static final String DEFAULT = "";
//     private static final String BRIGHT_WHITE = "1";
//     private static final String GREY = "30;1";
//     private static final String RED = "31";
//     private static final String GREEN = "32";
//     private static final String ORANGE = "33";
//     private static final String MAGENTA = "35";
//     private static final String CYAN = "36";
//     private static final String BRIGHT_MAGENTA = "35;1";
//
//     private static final char[] UNICODE_CHARSET = {'│', '├', '└', '─', '┊'};
//     private static final char[] ASCII_CHARSET   = {'|', '+', '\\', '-', '|'};
//     private static final int VERTICAL_CH = 0;
//     private static final int INTERSECT_CH = 1;
//     private static final int CORNER_CH = 2;
//     private static final int HORIZONTAL_CH = 3;
//     private static final int VERTICAL_DOTTED_CH = 4;
//
//     static {
//         AnsiConsole.systemInstall();
//     }
//
//     private char[] charSet = UNICODE_CHARSET;
//     private int column = 0;
//     private int terminalWidth;
//     private PrintStream out;
//
//     public static TreeViewer withAnsi()
//     {
//         var out = AnsiConsole.out();
//         out.setMode(AnsiMode.Force);
//         return new TreeViewer(out);
//     }

//     public TreeViewer(PrintStream out)
//     {
//         terminalWidth = AnsiConsole.getTerminalWidth();
//         if(terminalWidth < 1)
//         {
//             terminalWidth = 80;
//         }
//         this.out = out;
//     }
//
//     public TreeViewer ascii(boolean ascii)
//     {
//         charSet = ascii ? ASCII_CHARSET : UNICODE_CHARSET;
//         return this;
//     }

    private Output out;

    public TreeViewer(Output out)
    {
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
            out.println("No inheritance found", BRIGHT_MAGENTA);
        }
        else
        {
            out.println("Inheritance tree(s)", BRIGHT_MAGENTA);
            out.println("(Note: subclasses will appear multiple times if using multiple inheritance.)");
            for(var type : rootTypes)
            {
                viewNodeTree("    ", "    ", "    ", type, null);
                out.newLine();
            }
        }

        if(!nonInheritanceTypes.isEmpty())
        {
            out.println("Types without inheritance (no super/subclasses)", BRIGHT_MAGENTA);
            for(var type: nonInheritanceTypes)
            {
                viewNodeTree("    ", "    ", "    ", type, null);
                out.newLine();
            }
        }
    }

    private void viewNodeTree(String abovePrefix, String connectingPrefix, String belowPrefix, TypeNode type, TypeNode parent)
    {
        out.println(abovePrefix);

        List<TypeNode> otherParents = new ArrayList<>(type.getParents());
        otherParents.remove(parent);
        if(otherParents.size() > 0)
        {
            otherParents.sort(null);
            out.println(abovePrefix);
            for(var otherParent : otherParents)
            {
                out.print(abovePrefix);
                out.print(otherParent.isClass() ? "class " : "interface ", GREY);
                out.println(otherParent.getName(), GREY);
            }
            out.print(abovePrefix);
            out.println(out.chars(VERTICAL_DOTTED_CH), GREY);
        }

        out.print(connectingPrefix);
        if(type.isClass())
        {
            out.print("class ", GREEN);
        }
        else
        {
            out.print("interface ", RED);
        }
        out.print(type.getName(), BRIGHT_WHITE);

        var typeDefn = type.getDefinition();
        if(typeDefn != null)
        {
            typeDefn.getTypeParams().ifPresent(tp -> out.print(tp, GREY));
        }

        var sourceFile = type.getSourceFile();
        if(sourceFile == null)
        {
            out.println(" [no source]", GREY);
        }
        else
        {
            var packageName = sourceFile.getPackage();
            if(packageName == null)
            {
                out.newLine();
            }
            else
            {
                out.printRight("[package " + packageName + "]", GREY);
            }
        }

        var children = type.getChildren();
        var nChildren = children.size();
        viewMethods(belowPrefix + ((nChildren > 0) ? (out.chars(VERTICAL_CH) + "   ") : "    "), type);

        int i = 0;
        for(var child : children)
        {
            if(i < nChildren - 1)
            {
                viewNodeTree(belowPrefix + out.chars(VERTICAL_CH) + "   ",
                             belowPrefix + out.chars(INTERSECT_CH, HORIZONTAL_CH, HORIZONTAL_CH) + " ",
                             belowPrefix + out.chars(VERTICAL_CH) + "   ",
                             child, type);
            }
            else
            {
                viewNodeTree(belowPrefix + out.chars(VERTICAL_CH) + "   ",
                             belowPrefix + out.chars(CORNER_CH, HORIZONTAL_CH, HORIZONTAL_CH) + " ",
                             belowPrefix + "    ",
                             child, type);
            }
            i++;
        }
    }

    private void viewMethods(String prefix, TypeNode type)
    {
        var allMethods = new ArrayList<>(type.getMethods());
        if(allMethods.isEmpty())
        {
            out.print(prefix);
            if(type.getSourceFile() == null)
            {
                out.println("[methods unknown]", GREY);
            }
            else
            {
                out.println("[no methods]", GREY);
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
                // var modifiers = new TreeSet<>(defn.getModifiers());
                var modifiers = new TreeSet<String>();
                defn.getModifiers().forEach(m -> modifiers.add(m.toString()));

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

                    if(!type.isClass() && !modifiers.contains("default"))
                    {
                        modifiers.add("abstract");
                    }

                    // var typeParams = defn.getTypeParams();
                    var params     = method.getParameters();

                    // typeParamWidth  = max(typeParamWidth, (typeParams == null) ? 0 : typeParams.length() + 1);
                    typeParamWidth  = max(typeParamWidth, defn.getTypeParams().map(tp -> tp.length() + 1).orElse(0));
                    modifierWidth   = max(modifierWidth, modifiers.stream().mapToInt(s -> s.length() + 1).sum());
                    returnTypeWidth = max(returnTypeWidth, defn.getReturnType().map(rt -> rt.length() + 1).orElse(0));
                    signatureWidth  = max(
                        signatureWidth,
                        defn.getName().length() + 2 + (
                            params.isEmpty()
                            ? 2
                            // : params.stream().mapToInt(
                            //     p -> p.getType().getName().orElse(p.getName()).length() + 2).sum()
                            : params.stream().mapToInt(p -> paramStr(p).length() + 2).sum()
                        ));
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
                        out.println(prefix);
                    }
                    out.print(prefix);

                    out.startCount();
                    for(var mod : modifiers)
                    {
                        out.print(mod, (mod.equals("@Override") || mod.equals("abstract")) ? MAGENTA : ORANGE);
                        out.print(" ");
                    }
                    out.print(" ".repeat(modifierWidth - out.getNChars()));

                    out.startCount();
                    defn.getTypeParams().ifPresent(tp -> out.print(tp, GREY));
                    out.print(" ".repeat(typeParamWidth - out.getNChars()));

                    out.startCount();
                    defn.getReturnType().ifPresent(rt -> out.print(rt, GREY));
                    out.print(" ".repeat(returnTypeWidth - out.getNChars()));

                    out.startCount();
                    out.print(method.getName());
                    out.print("(");
                    out.printJoin(", ",
                                  DEFAULT,
                                  method.getParameters().stream().map(this::paramStr).toList(),
                                  GREY);
                    out.print(")");
                    out.print(" ".repeat(signatureWidth - out.getNChars()));

                    var overriddenBy = method.getOverriddenBy();
                    if(!overriddenBy.isEmpty())
                    {
                        var overriddingTypeList = new ArrayList<>(overriddenBy.stream().map(m -> m.getType().getName()).toList());
                        overriddingTypeList.sort(null);
                        out.print("[overridden in ", GREY);
                        out.printJoin(", ", GREY, overriddingTypeList, CYAN);
                        out.print("]", GREY);
                    }
                    out.newLine();
                }
            }
            if(nPrivateMethods > 0)
            {
                out.print(prefix);
                out.println("[" + nPrivateMethods + " private method" + ((nPrivateMethods == 1) ? "" : "s") + "]", GREY);
            }
        }
    }

    private String paramStr(ParameterNode p)
    {
        return p.getType().map(t -> t.getName()).orElseGet(() -> p.getName());
    }
}
