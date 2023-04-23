package edu.curtin.polyfind;
import edu.curtin.polyfind.definitions.*;
import edu.curtin.polyfind.tree.*;

import picocli.CommandLine;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "polyfill",
                     mixinStandardHelpOptions = true,
                     version = "0.1",
                     description = "Experimental tool to display the structure of a student's Java application.")
public class PolyFind implements Callable<Integer>
{
    public static void main(String[] args) 
    {
        System.exit(new CommandLine(new PolyFind()).execute(args));
    }
    
    @CommandLine.Parameters(index = "0", description = "Root of the source code directory tree.")
    private File directory;
    
    @CommandLine.Option(names = {"-a", "--ascii"}, 
                        description = "Use standard ASCII symbols only (in case non-ASCII Unicode doesn't display properly).")
    private boolean ascii;
    
    @Override
    public Integer call() throws Exception
    {
        var parser = new JavaParser();
        var treeBuilder = new TreeBuilder();
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
        
        TreeViewer.withAnsi().ascii(ascii).view(treeBuilder.build());
        return 0;
    }
}
