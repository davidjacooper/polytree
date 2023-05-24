package edu.curtin.polyfind.view;
import edu.curtin.polyfind.tree.*;
import static edu.curtin.polyfind.view.Output.*;

import java.util.stream.*;

public class PythonCallRegexGenerator implements CallRegexGenerator
{
    @Override
    public void generate(Output out, Stream<MethodNode> methods)
    {
        out.print("'\\b(?<!def )(");
        out.printJoin("|", DEFAULT, methods.map(m -> m.getName()).distinct().toList(), CYAN);
        out.print(")\\s*\\(' ");
    }

    @Override
    public String getAckOptions()
    {
        return "--python";
    }

    @Override
    public String getGrepOptions()
    {
        return "--include='*.py'";
    }
}
