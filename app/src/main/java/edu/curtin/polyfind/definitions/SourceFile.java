package edu.curtin.polyfind.definitions;

import java.io.*;
import java.nio.file.*;

public class SourceFile
{
    public static SourceFile read(Project project, Path path) throws IOException
    {
        return new SourceFile(project, path, Files.readString(path));
    }

    private final Project project;
    private final Path path;
    private final String content;

    public SourceFile(Project project, Path path, String content)
    {
        this.project = project;
        this.path = path;
        this.content = content;
    }

    public Project getProject() { return project; }
    public Path getPath()       { return path; }
    public String getContent()  { return content; }
}

// package edu.curtin.polyfind.definitions;
// import edu.curtin.polyfind.languages.*;
//
// import java.io.*;
// import java.nio.file.*;
// import java.util.*;
// import java.util.regex.*;
//
// public class SourceFile extends ScopedDefinition
// {
//     public static SourceFile read(Path p, Language language) throws IOException
//     {
//         return new SourceFile(p.toString(), language, Files.readString(p));
//     }
//
//     private Language language;
//     private String content;
//     private Optional<Project> project    = Optional.empty();
//     private Optional<String> packageName = Optional.empty();
//     private String scope = "";
//
//     public SourceFile(String name, Language language, String content)
//     {
//         super(null, 0, content.length(), name, ScopeType.INVISIBLE);
//         this.language = language;
//         this.content = content;
//     }
//
//     public void setProject(Project project)
//     {
//         this.project = Optional.of(project);
//     }
//
//     public void setPackage(String packageName)
//     {
//         this.packageName = Optional.of(packageName);
//     }
//
//     public void setScope(String scope)
//     {
//         this.scope = scope;
//     }
//
//     public Language getLanguage()         { return language; }
//     public String getContent()            { return content; }
//     public Optional<Project> getProject() { return project; }
//     public Optional<String> getPackage()  { return packageName; }
//
//     @Override
//     public Optional<String> getPublicScope()
//     {
//         return Optional.of(scope);
//     }
//
//     // @Override
//     // public String getDisplayName()
//     // {
//     //     return scope;
//     // }
// }
