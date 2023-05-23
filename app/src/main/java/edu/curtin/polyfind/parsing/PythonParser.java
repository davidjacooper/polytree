package edu.curtin.polyfind.parsing;
import edu.curtin.polyfind.definitions.*;

import java.util.*;
import java.util.regex.*;

public class PythonParser extends Parser
{
    private static final Pattern MAIN_CENSOR_PATTERN;
    static {
        var string = "('''|'|\"\"\"|\")(\\\\.|(?!\\1).)*\1";
        var comment = "#[^\n]*";
        MAIN_CENSOR_PATTERN = Pattern.compile(comment + '|' + string);
    }

    private static final Pattern NEWLINE_ESCAPE_CENSOR_PATTERN = Pattern.compile(
        "\\\\\\n");

    private static final String NAME = "\\b[A-Za-z_][A-Za-z0-9_]*\\b";
    private static final String FQ_NAME = NAME + "([ \\t]*\\.[ \\t]*" + NAME + ")*";

    // private static final Pattern DECLARATION_PATTERN = Pattern.compile(
    //     "(?<decorators>(^[ \\t]*@[^\\n]*\\n)*)"
    //     + "^(?<indent>[ \\t]*)(?<kind>def|class)[ \\t]+"
    //     + "(?<name>" + NAME + ")[ \\t]*"
    //     + "(\\((?<params>([^()]|" + bracketExprRegex("\\(", "\\)") + ")*)\\)[ \\t]*)?"
    //     + "(->[ \\t](?<returnType>[^:]+?)[ \\t]*)?"
    //     + ":[ \\t]*"
    //     + "(\\S[^\\n]*|"
    //     +   "(\\s*\\n\\k<indent>[ \t](\\\\\\n|(?!\\b(def|class)\\b)[^\\n])*)*"
    //     +   "(?<tail>\\s*(\\z|\\n(?!\\k<indent>[ \\t])[ \\t]*\\S))"
    //     + ")",
    //     Pattern.MULTILINE
    // );

    // private static final Pattern DECLARATION_PATTERN = Pattern.compile(
    //     "(?<decorators>(^[ \\t]*@[^\\n]*\\n)*)"
    //     + "^(?<indent>[ \\t]*)(?<kind>def|class)[ \\t]+"
    //     + "(?<name>" + NAME + ")[ \\t]*"
    //     + "(\\((?<params>([^()]|" + bracketExprRegex("\\(", "\\)") + ")*)\\)[ \\t]*)?"
    //     + "(->[ \\t](?<returnType>[^:]+?)[ \\t]*)?"
    //     + ":[ \\t]*"
    //     + "(\\S[^\\n]*|"
    //     +   "(\\s*\\n\\k<indent>[ \t](\\\\\\n|(?!\\b(def|class)\\b)[^\\n])*+)*"
    //     +   "(?<tail>\\s*(\\z|\\n(?!\\k<indent>[ \\t])[ \\t]*\\S))"
    //     + ")",
    //     Pattern.MULTILINE
    // );

    private static final Pattern DECLARATION_PATTERN = Pattern.compile(
        "(?<decorators>(^[ \\t]*@[^\\n]*\\n)*)"
        + "^(?<indent>[ \\t]*)(?<kind>def|class)[ \\t]+"
        + "(?<name>" + NAME + ")[ \\t]*"
        + "(\\((?<params>([^()]|" + bracketExprRegex("\\(", "\\)") + ")*)\\)[ \\t]*)?"
        + "(->[ \\t](?<returnType>[^:]+?)[ \\t]*)?"
        + ":[ \\t]*"
        + "(?<body>\\S[^\\n]*|"
        +   "(\\s*\\n\\k<indent>[ \t][^\\n]*)*"
        + ")",
        Pattern.MULTILINE
    );

    private static final Pattern DECORATOR_NAME_PATTERN = Pattern.compile(FQ_NAME);

    // // Must be applied iteratively, to remove all {...} and (...).
    // private static final Pattern PARAM_PREPROCESSOR1 = Pattern.compile(
    //     "\\{[^\\{]*\\}|\\([^\\(]*\\)|"
    // );
    //
    // // Applied once (after PARAM_PREPROCESSOR1), to remove lambda parameter list declarations
    // private static final Pattern PARAM_PREPROCESSOR2 = Pattern.compile(
    //     "lambda\\b((?!\\blambda\\b)[^\\[:]|" + bracketExprRegex("\\[", "\\]") + ")*:"
    // );

    // Applied iteratively, to remove all "{...}", "(...)", "[...]" and "lambda...:", all of which
    // may introduce symbols that interfere with parameter parsing. (Note: we don't bother removing
    // the body of a lambda, because (a) we don't need to, and (b) it's far more difficult to parse.
    private static final Pattern PARAM_CENSOR_PATTERN = Pattern.compile(
        "(?x) \\{[^\\{]*\\}  |  \\([^\\(]*\\)  |  \\[[^\\[]*\\]  |  \\blambda\\b((?!\\blambda\\b)[^:])*:"
    );

    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?<name>" + NAME + ")"
        // + "([ \\t]*:[ \\t]*(?<type>" + FQ_NAME + "(" + bracketExprRegex("\\[", "\\]") + ")?))?"
        + "([ \\t]*:[ \\t]*(?<type>[^=,]+))?"
        + "([ \\t]*=[ \\t]*(?<defaultValue>[^,]+))?"
    );

    private static final Pattern SUPERTYPE_PATTERN = Pattern.compile(
        "(?<meta>\\bmetaclass[ \\t]*=[ \\t]*)?(?<main>[^,]+)"
    );

    private static final Set<String> ABSTRACT_SUPERTYPES =
        Set.of("ABC", "ABCMeta", "abc.ABC", "abc.ABCMeta");

    public PythonParser() {}

    @Override
    public String language()
    {
        return "Python";
    }

    // @Override
    // public void parse(SourceFile file)
    // {
    //     var content = new CensoredString(file.getContent());
    //     content.censor(MAIN_CENSOR_PATTERN);
    //
    //     var defnSet = new TreeSet<ScopedDefinition>((d1, d2) -> d1.getStartPos() - d2.getStartPos());
    //
    //     while(true)
    //     {
    //         System.out.printf("\n---start---\n%s---end---\n", content.censored());
    //         var matcher = content.matcher(DECLARATION_PATTERN);
    //         if(!matcher.find())
    //         {
    //             break;
    //         }
    //
    //         ScopedDefinition defn;
    //         switch(matcher.uncensoredGroup("kind").get())
    //         {
    //             case "class":
    //                 defn = makeTypeDefinition(file, matcher);
    //                 break;
    //
    //             case "def":
    //                 defn = makeMethodDefinition(file, matcher);
    //                 break;
    //
    //             default:
    //                 throw new AssertionError();
    //         }
    //
    //         addModifiers(defn, matcher);
    //
    //         defnSet.tailSet(defn, false).stream()
    //                .takeWhile(existingDefn -> existingDefn.getStartPos() <= defn.getEndPos())
    //                .forEach(defn::addNested);
    //
    //         defn.getNested().forEach(defnSet::remove);
    //         defnSet.add(defn);
    //
    //         if(matcher.hasGroup("tail"))
    //         {
    //             content.censor(matcher.start(), matcher.start("tail"));
    //         }
    //         else
    //         {
    //             content.censor(matcher);
    //         }
    //     }
    //
    //     defnSet.forEach(file::addNested);
    // }

    @Override
    public void parse(SourceFile file)
    {
        var content = new CensoredString(file.getContent());
        content.censor(MAIN_CENSOR_PATTERN);
        content.censor(NEWLINE_ESCAPE_CENSOR_PATTERN, ' ');

        // var defnSet = new TreeSet<ScopedDefinition>((d1, d2) -> d1.getStartPos() - d2.getStartPos());
        var defnList = new LinkedList<ScopedDefinition>();
        defnList.add(file);

        while(true)
        {
            var matcher = content.matcher(DECLARATION_PATTERN);
            if(!matcher.find())
            {
                break;
            }

            var start = matcher.start();
            while(defnList.getLast().getEndPos() < start)
            {
                defnList.removeLast();
            }
            var containing = defnList.getLast();

            ScopedDefinition defn;
            switch(matcher.uncensoredGroup("kind").get())
            {
                case "class":
                    defn = makeTypeDefinition(file, containing, matcher);
                    break;

                case "def":
                    defn = makeMethodDefinition(file, containing, matcher);
                    break;

                default:
                    throw new AssertionError();
            }

            // addModifiers(defn, matcher);

            // defnSet.tailSet(defn, false).stream()
            //        .takeWhile(existingDefn -> existingDefn.getStartPos() <= defn.getEndPos())
            //        .forEach(defn::addNested);
            //
            // defn.getNested().forEach(defnSet::remove);
            // defnSet.add(defn);
            // if(matcher.hasGroup("tail"))
            // {
            //     content.censor(matcher.start(), matcher.start("tail"));
            // }
            // else
            // {
            //     content.censor(matcher);
            // }

            // var start = defn.getStartPos();
            // while(defnList.getLast().getEndPos() < start)
            // {
            //     defnList.removeLast();
            // }
            // defnList.getLast().addNested(defn);
            containing.addNested(defn);
            defnList.add(defn);

            content.censor(start, matcher.start("body"));
        }

        // defnSet.forEach(file::addNested);
    }

    private static TypeDefinition makeTypeDefinition(SourceFile file,
                                                     ScopedDefinition containing,
                                                     CensoredString.Matcher matcher)
    {
        var defn = new TypeDefinition(
            file,
            matcher.start(),
            matcher.end(),
            matcher.uncensoredGroup("name").get(),
            "class"
        );

        addModifiers(defn, matcher);

        matcher.censoredGroup("params").ifPresent(paramStr ->
        {
            paramStr.censorIteratively(PARAM_CENSOR_PATTERN);
            var superTypeMatcher = paramStr.matcher(SUPERTYPE_PATTERN);

            while(superTypeMatcher.find())
            {
                var superType = superTypeMatcher.uncensoredGroup("main").get().strip();
                var abc = ABSTRACT_SUPERTYPES.contains(superType);
                if(abc)
                {
                    defn.addModifier(Modifier.ABSTRACT);
                }

                if(superTypeMatcher.hasGroup("meta"))
                {
                    defn.setMetaType(superType);
                }
                else if(!abc) // If the (non-metaclass) base class is abc.ABC, don't count it as
                              // a supertype (as that just clutters things up).
                {
                    defn.addSuperType(superType);
                }
            }
        });
        return defn;
    }

    private static MethodDefinition makeMethodDefinition(SourceFile file,
                                                         ScopedDefinition containing,
                                                         CensoredString.Matcher matcher)
    {
        var start = matcher.start();
        var name = matcher.uncensoredGroup("name").get();
        var defn = new MethodDefinition(file, start, matcher.end(), name);

        if(containing instanceof TypeDefinition && "__init__".equals(name))
        {
            defn.setConstructor(true);
        }

        addModifiers(defn, matcher);

        var paramStr = matcher.censoredGroup("params").get();
        paramStr.censorIteratively(PARAM_CENSOR_PATTERN);
        var paramMatcher = paramStr.matcher(PARAMETER_PATTERN);

        var first = true;
        while(paramMatcher.find())
        {
            var paramDefn = new ParameterDefinition(
                file,
                start + paramMatcher.start(),
                start + paramMatcher.end(),
                paramMatcher.uncensoredGroup("name").get()
            );
            if(first)
            {
                first = false;
                if(containing instanceof TypeDefinition && !defn.is(Modifier.STATIC))
                {
                    // The first parameter (self) of non-static methods (and class methods) is
                    // regarded as 'implicit'. We record it, but (typically) its display will be
                    // suppressed.
                    paramDefn.setImplicit(true);
                }
            }

            defn.addParameter(paramDefn);

            paramMatcher.uncensoredGroup("type").ifPresent(t -> paramDefn.setType(decodeType(t)));
            paramMatcher.uncensoredGroup("defaultValue").ifPresent(v ->
                paramDefn.setDefaultValue(v.strip()));
        }

        matcher.uncensoredGroup("returnType").ifPresent(t -> defn.setReturnType(decodeType(t)));
        return defn;
    }

    private static String decodeType(String type)
    {
        type = type.strip();
        if((type.startsWith("'''") && type.endsWith("'''")) ||
           (type.startsWith("\"\"\"") && type.endsWith("\"\"\"")))
        {
            return type.substring(3, type.length() - 3).strip();
        }
        else if((type.startsWith("'") && type.endsWith("'")) ||
                (type.startsWith("\"") && type.endsWith("\"")))
        {
            return type.substring(1, type.length() - 1).strip();
        }
        else
        {
            return type;
        }
    }


    private static void addModifiers(Definition defn, CensoredString.Matcher matcher)
    {
        var allDecorators = matcher.uncensoredGroup("decorators").get();
        if(allDecorators.isEmpty()) { return; }

        for(var decoratorStr : allDecorators.split("\n"))
        {
            var nameStr = decoratorStr.substring(decoratorStr.indexOf('@') + 1).stripLeading();
            if(nameStr.startsWith("abstractmethod") ||
               nameStr.startsWith("abc.abstractmethod"))
            {
                defn.addModifier(Modifier.ABSTRACT);
            }
            else if(nameStr.startsWith("classmethod"))
            {
                defn.addModifier(Modifier.CLASS_METHOD);
            }
            else if(nameStr.startsWith("staticmethod"))
            {
                defn.addModifier(Modifier.STATIC);
            }
            else
            {
                defn.addModifier(Modifier.named(decoratorStr.strip()));
            }

            // @property, @...setter?
        }
    }
}
