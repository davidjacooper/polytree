package edu.curtin.polyfind;
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
                     version = "0.1",
                     description = "Displays the inheritance structure of a student's Java application, and recommends ack/grep commands for finding polymorphic method call sites.",
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
            var treeBuilder = new TreeBuilder();
            try
            {
                for(var file : Files.walk(directory.toPath()).filter(Files::isRegularFile).toList())
                                    // .filter(p -> p.toString().endsWith(".java"))
                                    // .toList())
                {
                    var parser = Parser.of(file);
                    if(parser.isPresent())
                    {
                        // for(var defn : parser.get().parse(SourceFile.read(file)))
                        // {
                        //     treeBuilder.addDefinition(defn);
                        // }
                        var sourceFile = SourceFile.read(file);
                        parser.get().parse(sourceFile);
                        sourceFile.walk(TypeDefinition.class).forEach(treeBuilder::addDefinition);
                            // .forEach(defn ->
                            // {
                            //     if(defn instanceof TypeDefinition)
                            //     {
                            //         treeBuilder.addDefinition((TypeDefinition)defn);
                            //     }
                            // });
                    }

                    // Parser.of(file).ifPresent(parser ->
                    // {
                    //     var sourceFile = SourceFile.read(file);
                    //     parser.parse(sourceFile);
                    //     sourceFile.getNestedRecursive()
                    //         .forEach(defn ->
                    //         {
                    //             if(defn instanceof TypeDefinition)
                    //             {
                    //                 treeBuilder.addDefinition((TypeDefinition)defn);
                    //             }
                    //         });
                    // });
                }
            }
            catch(NoSuchFileException e)
            {
                System.err.println("Directory cannot be found: " + e.getMessage());
                return 1;
            }

            var typeNodes = treeBuilder.build();
            var output = Output.withAnsi().ascii(ascii);
            new TreeViewer(output).view(typeNodes);

            String path;
            try
            {
                path = directory.getCanonicalPath();
            }
            catch(IOException e)
            {
                path = directory.getAbsolutePath();
            }
            new SearchRecommender(output).useAck(!useGrep).showCommands(typeNodes, path);
            return 0;
        }
        catch(IOException e)
        {
            System.err.println("An IO error occurred: " + e.getMessage());
            return 1;
        }
    }
}
