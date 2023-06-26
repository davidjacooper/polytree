package edu.curtin.polyfind.definitions;
import edu.curtin.polyfind.languages.Language;

import java.util.*;
import java.util.stream.*;

public class Project extends ScopedDefinition
{
    private Language language;
    private List<SourceFile> sourceFiles = new ArrayList<>();

    public Project(String name, Language language)
    {
        super(name);
        this.language = language;
    }

    public void addSourceFile(SourceFile file)
    {
        sourceFiles.add(file);
    }

    public Stream<SourceFile> getSourceFiles()
    {
        return sourceFiles.stream();
    }

    @Override
    public String toString()
    {
        return String.format("`project %s (%s)`", getName(), language);
    }
}
