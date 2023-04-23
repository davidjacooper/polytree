package edu.curtin.polyfind;
import edu.curtin.polyfind.definitions.*;
import edu.curtin.polyfind.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PolyFind 
{
    public static void main(String[] args) 
    {
        if(args.length > 1)
        {
            System.err.println("Too many command-line parameters.");
            System.exit(1);
        }
        
        try
        {
            var parser = new JavaParser();
            var treeBuilder = new TreeBuilder();
            for(var file : Files.walk(Path.of((args.length == 0) ? "." : args[0]))
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
            
            TreeViewer.withAnsi().view(treeBuilder.build());

        }
        catch(IOException e)
        {
            System.err.println("IO Error: " + e.getMessage());
        }
    }
}
