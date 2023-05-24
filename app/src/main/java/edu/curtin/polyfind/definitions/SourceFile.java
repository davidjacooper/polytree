package edu.curtin.polyfind.definitions;
import edu.curtin.polyfind.languages.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class SourceFile extends ScopedDefinition
{
    public static SourceFile read(Path p, Language language) throws IOException
    {
        return new SourceFile(p.toString(), language, Files.readString(p));
    }

    private Language language;
    private String content;
    private Optional<String> packageName = Optional.empty();
    private String scope = "";

    public SourceFile(String name, Language language, String content)
    {
        super(null, 0, content.length(), name);
        this.language = language;
        this.content = content;
    }

    public void setPackage(String packageName)
    {
        this.packageName = Optional.of(packageName);
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public Language getLanguage()        { return language; }
    public String getContent()           { return content; }
    public Optional<String> getPackage() { return packageName; }

    @Override
    public Optional<String> getPublicScope()
    {
        return Optional.of(scope);
    }

    // @Override
    // public String getDisplayName()
    // {
    //     return scope;
    // }
}
