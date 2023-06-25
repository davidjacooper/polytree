package edu.curtin.polyfind.parsing;
import edu.curtin.polyfind.definitions.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Parses parts of Python code using regular expressions. We only parse a small fraction of the
 * actual language syntax, as necessary to reconstruct the large-scale structure of input code.
 *
 * PythonParser uses the following basic approach:
 *
 * 1. Apply a single giant regex (DECLARATION_PATTERN) to search the text for a method or type
 *    (class/interface) declaration. The regex will match the entire scope of the declaration as
 *    determined by its indentation.
 * 2. Record the details of this declaration.
 * 3. "Censor" the first line of this declaration, to hide it from further regex applications.
 *    (See CensoredString.java.)
 * 4. Repeat until no more declarations can be found.
 *
 * This discovers declarations top-to-bottom, even though many will overlap with one another. The
 * iterative regex application gives us recursive parsing capabilities, even though regexes are not
 * themselves recursive.
 *
 * Further notes:
 * - As a point of comparison, JavaParser finds declarations in a different order, from inner-most
 *   to outer-most.
 *
 * - Before anything else, we must also censor string literals and comments, to avoid being
 *   fooled by their contents.
 *
 * - PythonParser _does not validate_ anything. It is wildly permissive of various combinations of
 *   constructs that are illegal and even nonsensical. What it does not recognise it simply ignores.
 *
 */
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
    private static final String Q_NAME = NAME + "([ \\t]*\\.[ \\t]*" + NAME + ")*";

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "^[ \\t]*"
        + "(from[ \\t]+(?<relative>\\.*)(?<from>" + Q_NAME + ")?)?"
        + "import[ \\t]+((?<star>\\*)|\\(?(?<list>[^\\n]+)\\)?)"
    );

    private static final Pattern IMPORT_ELEMENT_PATTERN = Pattern.compile(
        "(?<source>" + Q_NAME + ")"
        + "([ \\t]+as[ \\t](?<alias>" + NAME + "))?"
    );

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

    private static final Pattern DECORATOR_NAME_PATTERN = Pattern.compile(Q_NAME);

    // Applied iteratively, to remove all "{...}", "(...)", "[...]" and "lambda...:", all of which
    // may introduce symbols that interfere with parameter parsing. (Note: we don't bother removing
    // the body of a lambda, because (a) we don't need to, and (b) it's far more difficult to parse.
    private static final Pattern PARAM_CENSOR_PATTERN = Pattern.compile(
        "(?x) \\{[^\\{]*\\}  |  \\([^\\(]*\\)  |  \\[[^\\[]*\\]  |  \\blambda\\b((?!\\blambda\\b)[^:])*:"
    );

    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?<name>" + NAME + ")"
        // + "([ \\t]*:[ \\t]*(?<type>" + Q_NAME + "(" + bracketExprRegex("\\[", "\\]") + ")?))?"
        + "([ \\t]*:[ \\t]*(?<type>[^=,]+))?"
        + "([ \\t]*=[ \\t]*(?<defaultValue>[^,]+))?"
    );

    private static final Pattern SUPERTYPE_PATTERN = Pattern.compile(
        "(?<meta>\\bmetaclass[ \\t]*=[ \\t]*)?(?<main>[^,]+)"
    );

    private static final Set<List<String>> ABSTRACT_SUPERTYPES =
        Set.of(List.of("ABC"), List.of("ABCMeta"),
               List.of("abc", "ABC"), List.of("abc", "ABCMeta"));

    public PythonParser() {}

    @Override
    public void parse(Project project, SourceFile file)
    {
        // file.setScopeType(ScopeType.NAMESPACE);

        var content = new CensoredString(file.getContent());
        content.censor(MAIN_CENSOR_PATTERN);
        content.censor(NEWLINE_ESCAPE_CENSOR_PATTERN, ' ');

        var defnList = new LinkedList<ScopedDefinition>();
        defnList.add(findPackage(project, file));

        var defnBodies = new HashMap<ScopedDefinition,CensoredString>();

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

            containing.addNested(defn);
            defnList.add(defn);

            defnBodies.put(defn, matcher.censoredGroup("body").get());
            content.censor(start, matcher.start("body"));
        }

        // for(var defn : defnBodies.keySet())
        // {
        //     defn.addImportSuppliers(getImports(defnBodies.get(defn)));
        // }
    }

    // private static ScopedDefinition findPackage(Project project, Path path)
    // {
    //     ScopedDefinition scope = project;
    //
    //     var len = path.getNameCount();
    //     if(len > 1)
    //     {
    //         for(var pathComponent : path.subpath(0, len - 1))
    //         {
    //             var name = pathComponent.toString();
    //             scope = scope.getOrAddNested(name, () -> new PackageDefinition(name, "package"));
    //         }
    //     }
    //
    //     var name = path.toFile().getName().replaceAll("\\.py$", "");
    //     if(!name.equals("__init__")) // Package constructor
    //     {
    //         scope = scope.getOrAddNested(name, () -> new PackageDefinition(name, "module"));
    //     }
    //     return scope;
    // }

    private static ScopedDefinition findPackage(Project project, SourceFile file)
    {
        ScopedDefinition scope = project;

        var pathNames = new ArrayList<String>();
        file.getPath().forEach(p -> pathNames.add(p.toString()));

        var len = pathNames.size() - 1;
        var lastName = pathNames.get(len).replaceAll("\\.py$", "");
        pathNames.remove(len);

        for(var name : pathNames)
        {
            scope = scope.getOrAddNested(name, () -> new PackageDefinition(name, "package"));
        }

        if(!lastName.equals("__init__"))
        {
            scope = scope.getOrAddNested(lastName, () -> new PackageDefinition(lastName, "module"));
        }

        scope.setLocation(file, 0, file.getContent().length());
        return scope;
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
            TypeCategory.CLASS,
            "class"
        );

        addModifiers(defn, matcher);

        matcher.censoredGroup("params").ifPresent(paramStr ->
        {
            paramStr.censorIteratively(PARAM_CENSOR_PATTERN);
            var superTypeMatcher = paramStr.matcher(SUPERTYPE_PATTERN);

            while(superTypeMatcher.find())
            {
                // var superType = superTypeMatcher.uncensoredGroup("main").get().strip();
                // var abc = ABSTRACT_SUPERTYPES.contains(superType);
                // System.out.printf("superType=%s, abc=%s\n", superType, abc);
                // if(abc)
                // {
                //     defn.addModifier(Modifier.ABSTRACT);
                // }
                //
                // if(superTypeMatcher.hasGroup("meta"))
                // {
                //     defn.setMetaType(nameList(superType, "\\."), superType)
                //         .categoryHint(TypeCategory.CLASS)
                //         .constructHint("class");
                // }
                // else if(!abc) // If the (non-metaclass) base class is abc.ABC, don't count it as
                //               // a supertype (as that just clutters things up).
                // {
                //     defn.addSuperType(nameList(superType, "\\."), superType)
                //         .categoryHint(TypeCategory.CLASS)
                //         .constructHint("class");
                // }
                var superTypeDisplay = superTypeMatcher.uncensoredGroup("main").get().strip();
                var superType = nameList(superTypeDisplay, "\\.");
                var abc = ABSTRACT_SUPERTYPES.contains(superType);
                if(abc)
                {
                    defn.addModifier(Modifier.ABSTRACT);
                }

                if(superTypeMatcher.hasGroup("meta"))
                {
                    defn.setMetaType(superType, superTypeDisplay)
                        .categoryHint(TypeCategory.CLASS)
                        .constructHint("class");
                }
                else if(!abc) // If the (non-metaclass) base class is abc.ABC, don't count it as
                              // a supertype (as that just clutters things up).
                {
                    defn.addSuperType(superType, superTypeDisplay)
                        .categoryHint(TypeCategory.CLASS)
                        .constructHint("class");
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
                if(containing instanceof TypeDefinition && !defn.hasModifier(Modifier.STATIC))
                {
                    // The first parameter (self) of non-static methods (and class methods) is
                    // regarded as 'implicit'. We record it, but (typically) its display will be
                    // suppressed.
                    paramDefn.setImplicit(true);
                }
            }

            defn.addParameter(paramDefn);

            paramMatcher.uncensoredGroup("type").ifPresent(t ->
            {
                var typeStr = decodeType(t);
                paramDefn.setType(
                    new QualifiedTypeName(defn, nameList(typeStr, "\\."), typeStr, false)
                        .categoryHint(TypeCategory.CLASS)
                        .constructHint("class"));
            });
            paramMatcher.uncensoredGroup("defaultValue").ifPresent(v ->
                paramDefn.setDefaultValue(v.strip()));
        }

        matcher.uncensoredGroup("returnType").ifPresent(t ->
        {
            var typeStr = decodeType(t);
            defn.setReturnType(nameList(typeStr, "\\."), typeStr)
                .categoryHint(TypeCategory.CLASS)
                .constructHint("class");
        });
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
            // @dataclass?
        }
    }

    // private static List<PythonImportSupplier> getImports(CensoredString content)
    // {
    //     var matcher = content.matcher(IMPORT_PATTERN);
    //     while(matcher.find())
    //     {
    //         if(matcher.hasGroup("relative"))
    //         {
    //             var source = nameList(matcher.uncensoredGroup("from"));
    //         }
    //         else
    //         {
    //         }
    //     }
    // }

    @Override
    public void postParse(Project project)
    {
        // Trim the top of the package tree. Initially we assume that the entire file path represents the package hierarchy, but generally the package hierarchy may start at a lower branch of the file hierarchy.

        //
    }
}
