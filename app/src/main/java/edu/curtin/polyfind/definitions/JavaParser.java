package edu.curtin.polyfind.definitions;

import java.util.*;
import java.util.regex.*;

public final class JavaParser
{
    public static final String NAME = "\\b[A-Za-z_][A-Za-z0-9_]*";
    public static final String FQ_NAME = NAME + "(\\s*\\.\\s*" + NAME + ")*";
    
    private static final String TYPE_ARGS = "<([^<>]|<([^<>]|<([^<>]|<[^<>]>)*>)*>)*>";
    private static final String TYPE_USE = FQ_NAME + "(\\s*" + TYPE_ARGS + ")?(\\s*\\[\\s*\\])*";
    private static final String MODIFIERS = "\\b([a-z ]*)";

    private static final Pattern TYPE_DEF_PATTERN = Pattern.compile(
        "\\b(?<construct>class|interface)\\s+(?<name>" + NAME + ")\\s*(?<typeParams>" + TYPE_ARGS + 
        ")?(\\s*\\bextends\\s+(?<extends>" + TYPE_USE + "(\\s*,\\s*" + TYPE_USE + 
        ")*))?(\\s*\\bimplements\\s+(?<implements>" +  TYPE_USE + "(\\s*,\\s*" + TYPE_USE + ")*))?"
        + "\\s*\\{(?<body>[^{}]*)\\}" // Match only one scope
    );
    
    private static final Pattern INTERFACE_PATTERN = Pattern.compile(TYPE_USE);
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(?<modifiers>" + MODIFIERS + ")(?<typeParams>" + TYPE_ARGS + ")?\\s*\\b(?<returnType>" + TYPE_USE + ")\\s+(?<name>" + NAME + 
        ")\\s*\\(\\s*(?<params>(" + MODIFIERS + TYPE_USE + "\\s*\\b" + NAME +
        "(\\s*,\\s*" + MODIFIERS + TYPE_USE + "\\s*\\b" + NAME + ")*)?)\\)"
    );
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?<modifiers>" + MODIFIERS + ")(?<type>" + TYPE_USE + ")\\s*\\b(?<name>" + NAME + ")"
    );
    
    public JavaParser() {}
                
    public List<TypeDefinition> parse(SourceFile file)
    {
        var content = file.getPreprocessedContent();
        var results = new ArrayList<TypeDefinition>();
        int prevLength;
        do
        {
            var matcher = TYPE_DEF_PATTERN.matcher(content);
            while(matcher.find())
            {
                var name = matcher.group("name");
                var body = matcher.group("body")
                    .replaceAll("=.*?;", "");                // Remove initialisers
                    //.replaceAll("@\\s*[A-Za-z0-9_]+", "");  // Remove annotations
                    
                var methods = new ArrayList<MethodDefinition>();
                var methodMatcher = METHOD_PATTERN.matcher(body);
                while(methodMatcher.find())
                {
                    var methodName = methodMatcher.group("name");
                    if(!methodName.equals(name)) // Check that this isn't a constructor
                    {
                        var parameters = new ArrayList<ParameterDefinition>();
                        var paramMatcher = PARAMETER_PATTERN.matcher(methodMatcher.group("params"));
                        while(paramMatcher.find())
                        {
                            parameters.add(new ParameterDefinition(
                                new HashSet<>(List.of(paramMatcher.group("modifiers").trim().split(" +"))),
                                paramMatcher.group("type").replaceAll(" ", ""),
                                paramMatcher.group("name")));
                        }
                    
                        methods.add(new MethodDefinition(
                            new HashSet<>(List.of(methodMatcher.group("modifiers").trim().split(" +"))),
                            methodMatcher.group("typeParams"),
                            methodMatcher.group("returnType"),
                            methodName,
                            parameters
                        ));
                    }
                }
                
                String construct = matcher.group("construct");
                String superClass;
                String interfaces;
                if(construct.equals("class"))
                {
                    superClass = matcher.group("extends");
                    interfaces = matcher.group("implements");
                }
                else
                {
                    superClass = null;
                    interfaces = matcher.group("extends");
                }

                results.add(new TypeDefinition(
                    file,
                    matcher.group("construct"),
                    name,
                    matcher.group("typeParams"),
                    superClass,
                    (interfaces == null) ? List.of() : INTERFACE_PATTERN.matcher(interfaces).results().map(MatchResult::group).toList(),
                    methods
                ));
            }
                        
            // Discard another scope level
            prevLength = content.length();
            content = content.replaceAll("\\{[^{}]*\\}", "");
        }
        while(content.length() < prevLength);
        return results;
    }
}
