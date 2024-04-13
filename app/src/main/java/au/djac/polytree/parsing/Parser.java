package au.djac.polytree.parsing;
import au.djac.polytree.definitions.*;

import java.util.*;
import java.util.regex.*;

public abstract class Parser
{
    public static final int REGEX_RECURSE_DEPTH = 16;

    protected static String bracketExprRegex(String open, String close)
    {
        return
            (open + "(" + "[^" + open + close + "]|").repeat(REGEX_RECURSE_DEPTH)
            + open + "[^" + open + close + "]*+" + close
            + (")*+" + close).repeat(REGEX_RECURSE_DEPTH);
    }

    private static final Pattern NAME_LIST_DELETION = Pattern.compile("[\\[(<].*");

    protected static List<String> nameList(String combined, String delimiterPattern)
    {
        combined = NAME_LIST_DELETION.matcher(combined).replaceAll("");
        var list = new ArrayList<String>();
        for(var name : combined.split(delimiterPattern))
        {
            if(name.isEmpty())
            {
                throw new IllegalArgumentException("'" + combined + "'contains empty name component(s)");
            }
            list.add(name.strip());
        }
        return list;
    }

    public abstract void parse(Project project, SourceFile file);
    public void postParse(Project project) {}
}
