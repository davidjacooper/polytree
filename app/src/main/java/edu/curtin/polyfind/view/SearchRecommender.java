package edu.curtin.polyfind.view;
import edu.curtin.polyfind.tree.*;
import static edu.curtin.polyfind.view.Output.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class SearchRecommender
{
    private Output out;
    private boolean useAck = true;

    public SearchRecommender(Output out)
    {
        this.out = out;
    }

    public SearchRecommender useAck(boolean useAck)
    {
        this.useAck = useAck;
        return this;
    }

    public void showCommands(Collection<TypeNode> types, String path)
    {
        out.newLine();
        out.println("Commands for finding polymorphic method calls", BRIGHT_MAGENTA);
        out.println("(Copy and paste one of the following commands to find method call sites for a particular superclass/interface, or for all superclasses/interfaces at once.)");
        out.newLine();

        var superTypes = new ArrayList<>(types.stream().filter(t -> !t.getChildren().isEmpty()).toList());

        if(superTypes.isEmpty())
        {
            out.println("  [No superclasses or interfaces defined; no polymorphism is possible!]");
            return;
        }

        superTypes.sort(null);
        for(var type : superTypes)
        {
            var defn = type.getDefinition();
            if(defn == null) { continue; }

            if(type.isClass())
            {
                out.print("class ", GREEN);
            }
            else
            {
                out.print("interface ", RED);
            }
            out.print(type.getName(), BRIGHT_WHITE);

            // var typeParams = defn.getTypeParams();
            // if(typeParams != null)
            // {
            //     out.print(typeParams, GREY);
            // }
            defn.getTypeParams().ifPresent(tp -> out.print(tp, GREY));
            out.println(":");

            var methods = type.getMethods();
            if(methods.isEmpty())
            {
                out.println("  [No methods overridden in subclasses; no polymorphism is possible!]");
            }
            else
            {
                out.newLine();
                showCommand(methods, path);
            }
            out.newLine();
            out.newLine();
        }

        if(superTypes.size() > 1)
        {
            out.println("All together:", BRIGHT_WHITE);
            out.newLine();

            showCommand(superTypes.stream().flatMap(t -> t.getMethods().stream()).toList(), path);
            out.newLine();
        }
    }

    private void showCommand(List<MethodNode> methods, String path)
    {
        if(useAck)
        {
            out.print("  ack -C10 --java ");
        }
        else
        {
            out.print("  grep -nPR -C10 --color=auto ");
        }

        var methodPatterns = methods.stream()
            .map(m -> "(?<!" + m.getDefinition()
                                .getReturnType().orElse("") // FIXME: Java-specific
                                .replaceAll("\\[", "\\\\[")
                                .replaceAll("\\]", "\\\\]") + " )" + m.getName())
            .toList();

        out.print("'\\b(");
        out.printJoin("|", DEFAULT, methodPatterns, CYAN);
        out.print(")\\s*\\(' ");
        out.println("'" + path.replaceAll("'", "'\''") + "'", ORANGE);
    }
}
