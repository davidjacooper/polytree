package edu.curtin.polyfind;
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
                     description = "Experimental tool to display the structure of a student's Java application.",
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
            var parser = new JavaParser();
            var treeBuilder = new TreeBuilder();
            try
            {
                for(var file : Files.walk(directory.toPath())
                                    .filter(Files::isRegularFile)
                                    .filter(p -> p.toString().endsWith(".java"))
                                    .toList())
                {               
                    var sourceFile = SourceFile.read(file);
                    for(var defn : parser.parse(sourceFile))
                    {
                        treeBuilder.addDefinition(defn);
                    }
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
