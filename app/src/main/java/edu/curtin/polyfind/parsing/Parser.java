package edu.curtin.polyfind.parsing;
import edu.curtin.polyfind.definitions.*;

public abstract class Parser
{
    public static final int REGEX_RECURSE_DEPTH = 16;
    protected static String bracketExprRegex(String open, String close)
    {
        return
            (open + "(" + "[^" + open + close + "]|").repeat(REGEX_RECURSE_DEPTH)
            + open + "[^" + open + close + "]*" + close
            + (")*" + close).repeat(REGEX_RECURSE_DEPTH);
    }

    public abstract void parse(SourceFile file);
}
