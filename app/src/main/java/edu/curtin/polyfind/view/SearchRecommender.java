package edu.curtin.polyfind.view;
import edu.curtin.polyfind.languages.*;
import edu.curtin.polyfind.tree.*;
import static edu.curtin.polyfind.view.Output.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class SearchRecommender
{
    private final Output out;
    private final Language language;
    private boolean useAck = true;

    public SearchRecommender(Output out, Language language)
    {
        this.out = out;
        this.language = language;
    }

    public SearchRecommender useAck(boolean useAck)
    {
        this.useAck = useAck;
        return this;
    }

    public void showCommands(Collection<TypeNode> types, String path)
    {
        out.newLine();
        out.println("Commands for finding polymorphic " + language.getName() + " method calls", BRIGHT_MAGENTA);
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
            type.getDefinition().ifPresent(defn ->
            {
                out.print(type.getConstruct(), Common.TYPE_COLOURS.get(type.getCategory()));
                out.print(" ");
                out.print(type.getName(), BRIGHT_WHITE);

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
            });
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
        var gen = language.getCallRegexGenerator();

        if(useAck)
        {
            out.print("  ack -C10 ");
            out.print(gen.getAckOptions());
            out.print(" ");
        }
        else
        {
            out.print("  grep -nPR -C10 --color=auto ");
            out.print(gen.getGrepOptions());
            out.print(" ");
        }

        gen.generate(out, methods);
        out.println("'" + path.replaceAll("'", "'\''") + "'", ORANGE);
    }
}
