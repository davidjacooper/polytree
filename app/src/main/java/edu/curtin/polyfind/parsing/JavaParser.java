package edu.curtin.polyfind.parsing;
import edu.curtin.polyfind.definitions.*;

import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Parses parts of Java code using regular expressions. We only parse a small fraction of the
 * actual Java language syntax, as necessary to reconstruct the large-scale structure of input code.
 *
 * JavaParser uses the following basic approach:
 *
 * 1. Apply a single giant regex (DECLARATION_PATTERN) to search the text for a method or type
 *    (class/interface) declaration. Specifically, we look for one that contains _no further_ such
 *    declarations.
 * 2. Record the details of this declaration.
 * 3. "Censor" the matched text to hide this declaration from further regex applications.
 *    (See CensoredString.java.)
 * 4. Repeat until no more declarations can be found.
 *
 * This discovers the most deeply-nested declarations first (which genuinely don't contain any other
 * declarations), then moves "outwards" to their containing declarations, which can be found once
 * the inner declarations have been censored (hidden). This gives us recursive parsing capabilities,
 * even though regexes are not themselves recursive. The ordering also lets us discover the nesting
 * relationships among declarations.
 *
 * As a technical detail, we must also intermittently censor other occurrences of brace pairs {...},
 * because the main regex is prevented from containing these (in un-censored form) within its body.
 *
 * Further notes:
 * - As a point of comparison, PythonParser operates in a top-to-bottom order, finding declarations
 *   in the order they appear in the code.
 *
 * - Before anything else, we must also censor string literals and comments, to avoid being
 *   fooled by their contents.
 *
 * - JavaParser _does not validate_ anything. It is wildly permissive of various combinations of
 *   constructs that are illegal and even nonsensical. What it does not recognise it simply ignores.
 *
 */
public class JavaParser extends Parser
{
    private static final Pattern MAIN_CENSOR_PATTERN = Pattern.compile(
        "//[^\n]*"                       // single line comments
        + "|/\\*([^*]|\\*[^/])*\\*?\\*/" // multi-line comments
        + "|\"([^\"\\\\]|\\\\.)*\""      // strings
    );

    // // private static final Pattern INITIALISER_CENSOR_PATTERN = Pattern.compile(
    // //     "(?<=)([^;({[]|"
    // //     + bracketExprRegex("\\(", "\\)") + "|"
    // //     + bracketExprRegex("\\[", "\\]") + "|"
    // //     + bracketExprRegex("\\{", "\\}") + ")*?(?=;)"
    // // );
    // private static final Pattern INITIALISER_CENSOR_PATTERN = Pattern.compile(
    //     "(?<=)([^;({[]|"
    //     + bracketExprRegex("\\(", "\\)") + "|"
    //     + bracketExprRegex("\\[", "\\]") + ")*?(?=;)"
    // );

    private static final Pattern SCOPE_CENSOR_PATTERN = Pattern.compile("\\{[^{}]*\\}");

    private static final String NAME = "\\b[A-Za-z_][A-Za-z0-9_]*\\b";
    private static final String FQ_NAME = NAME + "(\\s*\\.\\s*" + NAME + ")*";

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "(^|\\b)package\\s+(?<name>" + FQ_NAME + ")\\s*;");

    private static final String TYPE_ARGS = bracketExprRegex("<", ">");
    private static final String ANNOTATION =
        "@\\s*" + FQ_NAME + "(\\s*" + bracketExprRegex("\\(", "\\)") + ")?\\s*";
    private static final String STD_MODIFIER =
        "(\\b(abstract|default|final|native|non-sealed|open|private|protected|public|static|"
        + "sealed|strictfp|synchronized|transient|volatile)\\b)";

    private static final String TYPE_USE =
        FQ_NAME
        + "(\\s*" + TYPE_ARGS + ")?"
        + "(\\s*(" + ANNOTATION + ")*\\[\\s*\\])*";

    private static final String ANNOTATABLE_TYPE_USE =
        "(" + ANNOTATION + ")*" + TYPE_USE;

    private static final String TYPE_LIST =
        ANNOTATABLE_TYPE_USE + "(\\s*,\\s*" + ANNOTATABLE_TYPE_USE + ")*";

    private static final List<String> RESERVED = List.of("abstract", "assert", "boolean", "break",
        "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double",
        "else", "enum", "extends", "final", "finally", "float", "for", "if", "goto", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
        "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
        "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while");

    private static final List<String> RESERVED_NON_TYPES = List.of("abstract", "assert", "break",
        "case", "catch", "class", "const", "continue", "default", "do", "else", "enum", "extends",
        "final", "finally", "for", "if", "goto", "implements", "import", "instanceof", "interface",
        "native", "new", "package", "private", "protected", "public", "return", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "volatile", "while");

    private static String notReservedRegex(List<String> reserved)
    {
        return "(?!\\b" + String.join("\\b)(?!\\b", reserved) + "\\b)";
    }

    private static final Pattern TYPE_PATTERN = Pattern.compile(TYPE_USE);

    private static final Pattern MODIFIER_PATTERN = Pattern.compile(
        ANNOTATION + "|" + STD_MODIFIER);

    private static final String MODIFIERS =
        "(?<modifiers>(" + ANNOTATION + "|" + STD_MODIFIER + "\\s*)*)";

    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        MODIFIERS
        + "(?<type>" + notReservedRegex(RESERVED_NON_TYPES) + TYPE_USE + ")\\s*"
        + "(?<vararg>(" + ANNOTATION + ")*\\.\\.\\.\\s*)?"
        + "(?<name>" + notReservedRegex(RESERVED) + NAME + ")"
    );

    private static final Pattern DECLARATION_PATTERN = Pattern.compile(
        MODIFIERS
        + "("
            + "(?<type>"
                + "\\b(?<construct>class|interface|record|enum)\\s+"
                + "(?<typeName>" + notReservedRegex(RESERVED) + NAME + ")\\s*"
                + "(?<typeTypeParams>" + TYPE_ARGS + ")?\\s*"
                + "(?<recordHeader>" + bracketExprRegex("\\(", "\\)") + "\\s*)?"
                + "(\\bextends\\s+(?<extends>" + TYPE_LIST + ")\\s*)?"
                + "(\\bimplements\\s+(?<implements>" + TYPE_LIST + ")\\s*)?"
                + "(\\bpermits\\s+(?<permits>" + TYPE_LIST + ")\\s*)?"
                + "(\\{[^{}]*\\})"
            + ")|"
            + "(?<anon>"
                + "\\bnew\\s+"
                + "(?<superType>" + ANNOTATABLE_TYPE_USE + ")"
                + "(?<anonTypeArgs>" + TYPE_ARGS + ")?\\s*"
                + "\\((?<anonArgs>([^()]|" + bracketExprRegex("\\(", "\\)") + ")*)\\)\\s*"
                + "(\\{[^{}]*\\})"
            + ")|"
            + "(?<method>"
                + "(?<methodTypeParams>" + TYPE_ARGS + ")?\\s*"
                + "(?<returnType>" + notReservedRegex(RESERVED_NON_TYPES) + TYPE_USE + ")\\s+"
                + "(?<methodName>" + notReservedRegex(RESERVED) + NAME + ")\\s*"
                + "\\((?<params>([^()]|" + bracketExprRegex("\\(", "\\)") + ")*)\\)\\s*"
                + "(throws\\s+(?<throws>" + TYPE_LIST + ")*\\s*)?"
                + "(;|\\{[^{}]*\\})"
            + ")"
        + ")"
    );

    public JavaParser() {}

    // @Override
    // public String language()
    // {
    //     return "Java";
    // }

    @Override
    public void parse(SourceFile file)
    {
        var content = new CensoredString(file.getContent());
        content.censor(MAIN_CENSOR_PATTERN);

        var pkgMatcher = content.matcher(PACKAGE_PATTERN);
        if(pkgMatcher.find())
        {
            pkgMatcher.uncensoredGroup("name").ifPresent(file::setPackage);
        }

        var defnSet = new TreeSet<ScopedDefinition>((d1, d2) -> d1.getStartPos() - d2.getStartPos());

        while(true)
        {
            var matcher = content.matcher(DECLARATION_PATTERN);
            if(!matcher.find())
            {
                if(content.censor(SCOPE_CENSOR_PATTERN)) { continue; }
                else                                     { break; }
            }

            ScopedDefinition defn;
            if(matcher.hasGroup("type"))      { defn = makeNamedTypeDefinition(file, matcher); }
            else if(matcher.hasGroup("anon")) { defn = makeAnonTypeDefinition(file, matcher); }
            else                              { defn = makeMethodDefinition(file, matcher); }

            addModifiers(defn, matcher);

            var construct = matcher.uncensoredGroup("construct").orElse("");
            var isInterface = "interface".equals(construct);
            var isClass = !isInterface && !construct.isEmpty();

            defnSet
                .tailSet(defn, false)
                .stream()
                .takeWhile(existingDefn -> existingDefn.getStartPos() <= defn.getEndPos())
                .forEach(existingDefn ->
                {
                    if(isInterface)
                    {
                        // Everything in an interface is public.
                        existingDefn.addModifier(Modifier.PUBLIC);

                        // All non-default instance methods in an interface are abstract.
                        if(existingDefn instanceof MethodDefinition &&
                           !existingDefn.is(Modifier.STATIC) &&
                           !existingDefn.is(Modifier.DEFAULT))
                        {
                            existingDefn.addModifier(Modifier.ABSTRACT);
                        }
                    }
                    else if(isClass && !(existingDefn.is(Modifier.PRIVATE) ||
                                         existingDefn.is(Modifier.PROTECTED) ||
                                         existingDefn.is(Modifier.PUBLIC)))
                    {
                        existingDefn.addModifier(Modifier.PACKAGE_PRIVATE);
                    }
                    defn.addNested(existingDefn);
                });
            defn.getNested().forEach(defnSet::remove);
            defnSet.add(defn);
            content.censor(matcher);
        }

        defnSet.forEach(file::addNested);
    }

    private static TypeDefinition makeNamedTypeDefinition(SourceFile file,
                                                          CensoredString.Matcher matcher)
    {
        var defn = new TypeDefinition(
            file,
            matcher.start(),
            matcher.end(),
            matcher.uncensoredGroup("typeName").get(),
            matcher.uncensoredGroup("construct").get()
        );
        matcher.uncensoredGroup("typeTypeParams").ifPresent(defn::setTypeParams);

        Stream.of("extends", "implements")
              .flatMap(kw -> matcher.censoredGroup(kw).stream())
              .flatMap(s -> s.matcher(TYPE_PATTERN).resultsUncensored())
              .forEach(defn::addSuperType);
        return defn;
    }

    private static TypeDefinition makeAnonTypeDefinition(SourceFile file,
                                                         CensoredString.Matcher matcher)
    {
        var start = matcher.start();
        var defn = new TypeDefinition(
            file,
            start,
            matcher.end(),
            String.format("[anonymous class at %s:%s]", file.getName(), start),
            "class"
        );
        matcher.uncensoredGroup("superType").ifPresent(defn::addSuperType);
        return defn;
    }

    private static MethodDefinition makeMethodDefinition(SourceFile file,
                                                         CensoredString.Matcher matcher)
    {
        var start = matcher.start();
        var defn = new MethodDefinition(
            file,
            start,
            matcher.end(),
            matcher.uncensoredGroup("methodName").get()
        );

        matcher.uncensoredGroup("methodTypeParams").ifPresent(defn::setTypeParams);
        matcher.uncensoredGroup("returnType").ifPresent(defn::setReturnType);

        var paramMatcher = matcher.censoredGroup("params").get().matcher(PARAMETER_PATTERN);
        while(paramMatcher.find())
        {
            var paramDefn = new ParameterDefinition(
                file,
                start + paramMatcher.start(),
                start + paramMatcher.end(),
                paramMatcher.uncensoredGroup("name").get()
            );
            defn.addParameter(paramDefn);
            paramMatcher.uncensoredGroup("type").ifPresent(paramDefn::setType);
            addModifiers(paramDefn, paramMatcher);
        }

        matcher.censoredGroup("throws").stream()
            .flatMap(s -> s.matcher(TYPE_PATTERN).resultsUncensored())
            .forEach(defn::addCheckedException);

        return defn;
    }


    private static void addModifiers(Definition defn, CensoredString.Matcher matcher)
    {
        matcher.censoredGroup("modifiers").get().matcher(MODIFIER_PATTERN).resultsUncensored()
            .forEach(s ->
            {
                defn.addModifier(Modifier.named(s.strip()));
            });
    }
}
