package edu.curtin.polyfind.parsing;
import edu.curtin.polyfind.definitions.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public abstract class Parser
{
    public static final int REGEX_RECURSE_DEPTH = 16;

    public abstract String language();
    // public abstract List<TypeDefinition> parse(SourceFile file);
    public abstract void parse(SourceFile file);

    public static Optional<Parser> of(Path path)
    {
        var name = path.toFile().getName().toLowerCase();
        if(name.startsWith("._"))
        {
            // Ignore MacOS metadata files
            return Optional.empty();
        }

        if(name.endsWith(".java"))
        {
            return Optional.of(new JavaParser());
        }
        if(name.endsWith(".py"))
        {
            return Optional.of(new PythonParser());
        }
        return Optional.empty();
    }

    protected static String bracketExprRegex(String open, String close)
    {
        return
            (open + "(" + "[^" + open + close + "]|").repeat(REGEX_RECURSE_DEPTH)
            + open + "[^" + open + close + "]*" + close
            + (")*" + close).repeat(REGEX_RECURSE_DEPTH);
    }

    // protected static String replace(MatchResult r)
    // {
    //     var len = r.end() - r.start();
    //     if(r.group().startsWith("\"") && len >= 2)
    //     {
    //         return "\"" + " ".repeat(len - 2) + "\"";
    //     }
    //     else
    //     {
    //         return " ".repeat(len);
    //     }
    // }
    // protected static String censor(MatchResult r)
    // {
    //     return " ".repeat(r.end() - r.start());
    // }
}