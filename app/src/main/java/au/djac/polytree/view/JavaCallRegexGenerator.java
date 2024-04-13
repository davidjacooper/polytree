package au.djac.polytree.view;
import au.djac.polytree.tree.*;
import static au.djac.polytree.view.Output.*;

import java.util.stream.*;

public class JavaCallRegexGenerator implements CallRegexGenerator
{
    @Override
    public void generate(Output out, Stream<MethodNode> methods)
    {
        var methodPatterns = methods
            .map(m -> "(?<!" + m.getDefinition()
                                .getReturnType()
                                .map(Object::toString)
                                .orElse("")
                                .replaceAll("\\[", "\\\\[")
                                .replaceAll("\\]", "\\\\]") + " )" + m.getName())
            .toList();

        out.print("'\\b(");
        out.printJoin("|", DEFAULT, methodPatterns, CYAN);
        out.print(")\\s*\\(' ");
    }

    @Override
    public String getAckOptions()
    {
        return "--java";
    }

    @Override
    public String getGrepOptions()
    {
        return "--include='*.java'";
    }
}
