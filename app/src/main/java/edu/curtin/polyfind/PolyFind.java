package edu.curtin.polyfind;
import edu.curtin.polyfind.languages.*;
import edu.curtin.polyfind.parsing.*;
import edu.curtin.polyfind.definitions.*;
import edu.curtin.polyfind.tree.*;
import edu.curtin.polyfind.view.*;

import picocli.CommandLine;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "polyfind",
                     mixinStandardHelpOptions = true,
                     version = "0.2",
                     description = "Displays the inheritance structure of a student's Java/Python application, and recommends ack/grep commands for finding polymorphic method call sites.",
                     footer = "Copyright (c) 2023 by David J A Cooper.")
public class PolyFind implements Callable<Integer>
{
    public static void main(String[] args)
    {
        System.exit(new CommandLine(new PolyFind()).execute(args));
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
            var treeBuilder = new TreeBuilder();
            try
            {
                Files.walk(directory.toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.toFile().getName().startsWith("._"))
                    .forEach(path ->
                    {
                        languageSet.getByPath(path).ifPresent(language ->
                        {
                            try
                            {
                                var sourceFile = SourceFile.read(path, language);
                                language.getParser().parse(sourceFile);
                                sourceFile.walk(TypeDefinition.class).forEach(treeBuilder::addDefinition);
                            }
                            catch(IOException e)
                            {
                                throw new UncheckedIOException(e);
                            }
                        });
                    });
            }
            catch(NoSuchFileException e)
            {
                System.err.println("Directory cannot be found: " + e.getMessage());
                return 1;
            }

            var typeNodes = treeBuilder.build();
            var output = Output.withAnsi().ascii(ascii);
            new TreeViewer(output).view(typeNodes);

            languageSet.getAll().forEach(language ->
                new SearchRecommender(output, language)
                    .useAck(!useGrep)
                    .showCommands(typeNodes, getAbsPath(directory))
            );
            return 0;
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
