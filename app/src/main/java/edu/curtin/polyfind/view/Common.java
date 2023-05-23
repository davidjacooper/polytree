package edu.curtin.polyfind.view;
import edu.curtin.polyfind.tree.*;

public class Common
{
    public static String construct(TypeNode t)
    {
        return t.getDefinition()
            .map(d -> d.getConstruct())
            .orElseGet(() -> t.isClass() ? "class" : "interface");
    }
}
