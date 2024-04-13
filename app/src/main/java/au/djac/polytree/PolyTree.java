package au.djac.polytree;
import au.djac.polytree.languages.*;
import au.djac.polytree.parsing.*;
import au.djac.polytree.definitions.*;
import au.djac.polytree.tree.*;
import au.djac.polytree.view.*;

import picocli.CommandLine;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "polytree",
                     mixinStandardHelpOptions = true,
                     version = "0.2",
                     description = "Displays the inheritance structure of a student's Java/Python application, and recommends ack/grep commands for finding polymorphic method call sites.",
                     footer = "Copyright (c) 2023-2024 by David J A Cooper.")
public class PolyTree implements Callable<Integer>
{
    public static void main(String[] args)
    {
        System.exit(new CommandLine(new PolyTree()).execute(args));
    }

    @CommandLine.Parameters(index = "0", description = "Root of the source code directory tree to parse and display.")
    private File directory;

    @CommandLine.Option(names = {"-a", "--ascii"},
                        description = "Use standard ASCII symbols only (if non-ASCII box-drawing symbols don't display properly).")
    private boolean ascii;

    @CommandLine.Option(names = {"-g", "--grep"},
                        description = "Show usage for the 'grep' command (instead of for the 'ack' command by default).")
    private boolean useGrep;

    @Override
    public Integer call()
    {
        try
        {
            var languageSet = new LanguageSet();
            var projects = new HashMap<Language,Project>();
            Files.walk(directory.toPath())
                .filter(Files::isRegularFile)
                .filter(path -> !path.toFile().getName().startsWith("._"))
                .forEach(path ->
                {
                    languageSet.getByPath(path).ifPresent(language ->
                    {
                        try
                        {
                            var project = projects.computeIfAbsent(
                                language,
                                _l -> new Project(directory.toString(), language));

                            language.getParser().parse(project, SourceFile.read(project, path));
                        }
                        catch(IOException e)
                        {
                            throw new UncheckedIOException(e);
                        }
                    });
                });

            projects.forEach((language, project) ->
            {
                var treeBuilder = new TreeBuilder();
                project.walk(TypeDefinition.class).forEach(treeBuilder::addDefinition);

                var typeNodes = treeBuilder.build();
                var output = Output.withAnsi().ascii(ascii);
                new TreeViewer(output).view(typeNodes);

                new SearchRecommender(output, language)
                    .useAck(!useGrep)
                    .showCommands(typeNodes, getAbsPath(directory));
            });
            return 0;
        }
        catch(NoSuchFileException e)
        {
            System.err.println("Directory cannot be found: " + e.getMessage());
            return 1;
        }
        catch(IOException | UncheckedIOException e)
        {
            System.err.println("An IO error occurred: " + e.getMessage());
            return 1;
        }
    }

    private String getAbsPath(File directory)
    {
        try
        {
            return directory.getCanonicalPath();
        }
        catch(IOException e)
        {
            return directory.getAbsolutePath();
        }
    }
}
