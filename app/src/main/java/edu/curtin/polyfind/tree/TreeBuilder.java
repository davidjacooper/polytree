package edu.curtin.polyfind.tree;
import edu.curtin.polyfind.definitions.*;

import java.util.*;

public class TreeBuilder
{
    private Map<String,TypeNode> typeMap = new HashMap<>();

    public TreeBuilder() {}
    
    public void addDefinition(TypeDefinition defn)
    {
        typeMap.put(defn.getName(), new TypeNode(defn));
    }
    
    public Collection<TypeNode> build()
    {
        // Connect type nodes by inheritance.
        for(var node : new ArrayList<>(typeMap.values()))
        {
            var defn = node.getDefinition();            
            var superClass = defn.getRawSuperClass();
            if(superClass != null)
            {
                var superNode = typeMap.computeIfAbsent(superClass, name -> new TypeNode(name, true));
                node.addParent(superNode);
                superNode.addChild(node);
            }
            for(var intf : defn.getRawInterfaces())
            {
                var superNode = typeMap.computeIfAbsent(intf, name -> new TypeNode(name, false));
                node.addParent(superNode);
                superNode.addChild(node);
            }
        }
        
        // Connect methods and parameters. (Start a new loop here, because we want all the types 
        // added to typeMap in the above loop (if any) to be present.)
        for(var node : new ArrayList<>(typeMap.values()))
        {
            var defn = node.getDefinition();
            if(defn != null)
            {
                for(var methodDefn : defn.getMethods())
                {
                    var method = new MethodNode(methodDefn, node);
                    node.addMethod(method);
                    for(var parameterDefn : methodDefn.getParameters())
                    {
                        var parameterType = typeMap.computeIfAbsent(parameterDefn.getType(), name -> new TypeNode(name, true)); 
                            // (true/false -- we don't actually know whether this type is a class or interface.)
                            
                        method.addParameter(new ParameterNode(parameterDefn, method, parameterType));
                    }
                }
            }
        }
        
        // Find method overrides
        for(var type : typeMap.values())
        {
            if(type.getParents().size() == 0 && type.getChildren().size() > 0)
            {
                findMethodOverrides(type, Map.of());
            }
        }
        
        return typeMap.values();
    }
    
    private static void findMethodOverrides(TypeNode node, Map<String,MethodNode> superMethods)
    {
        // FIXME: does not account for multiple inheritance.
        
        var newSuperMethods = new HashMap<>(superMethods);
    
        for(var method : node.getMethods())
        {
            var name = method.getName();
            var superMethod = superMethods.get(name);
            if(superMethod != null)
            {
                var superModifiers = superMethod.getDefinition().getModifiers();
                if(!(superModifiers.contains("private") || superModifiers.contains("static"))) // Can override?
                {
                    method.setOverrides(superMethod);
                    superMethod.addOverriddenBy(method);
                }
            }
            
            var modifiers = method.getDefinition().getModifiers();
            if(!(modifiers.contains("final") || modifiers.contains("private") || modifiers.contains("static")))
            {
                newSuperMethods.put(name, method);
            }
        }
        
        for(var child : node.getChildren())
        {
            findMethodOverrides(child, newSuperMethods);
        }
    }
}
