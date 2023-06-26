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
