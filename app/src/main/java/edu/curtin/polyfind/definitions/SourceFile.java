package edu.curtin.polyfind.definitions;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

import java.util.regex.*;

public class SourceFile 
{
    private static final Pattern PREPROCESSOR;    
    static {
        var singleLineComment = "//[^\n]*";
        var multiLineComment = "/\\*([^*]|\\*[^/])*\\*?\\*/";
        var string = "\"([^\"\\\\]|\\\\.)*\"";        
        PREPROCESSOR = Pattern.compile(singleLineComment + '|' + multiLineComment + '|' + string);
    }
    
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "(^|\b)package\\s+(?<name>" + JavaParser.FQ_NAME + ")\\s*;");
        
    public static SourceFile read(Path p) throws IOException
    {
        return new SourceFile(p.toString(), Files.readString(p));
    }
    
    private String name;
    private String content;
    private String preprocessedContent = null;
    private String packageName = null;
    
    SourceFile(String name, String content) 
    {
        this.name = name;
        this.content = content;
    }
    
    public String getName() { return name; }
    public String getContent() { return content; }
    public String getPreprocessedContent() 
    { 
        if(preprocessedContent == null)
        {
            preprocessedContent = PREPROCESSOR.matcher(content).replaceAll("");
        }
        return preprocessedContent; 
    }
    
    public String getPackage()
    {
        if(packageName == null)
        {        
            var matcher = PACKAGE_PATTERN.matcher(getPreprocessedContent());
            if(matcher.find())
            {
                packageName = matcher.group("name");
            }
        }
        return packageName;
    }
}
