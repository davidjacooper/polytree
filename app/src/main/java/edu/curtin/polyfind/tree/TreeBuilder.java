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
        // for(var node : new ArrayList<>(typeMap.values()))
        // {
        //     var defn = node.getDefinition();
        //     var superClass = defn.getRawSuperClass();
        //     if(superClass != null)
        //     {
        //         var superNode = typeMap.computeIfAbsent(superClass, name -> new TypeNode(name, true));
        //         node.addParent(superNode);
        //         superNode.addChild(node);
        //     }
        //     for(var intf : defn.getRawInterfaces())
        //     {
        //         var superNode = typeMap.computeIfAbsent(intf, name -> new TypeNode(name, false));
        //         node.addParent(superNode);
        //         superNode.addChild(node);
        //     }
        // }
        for(var node : new ArrayList<>(typeMap.values()))
        {
            // for(var superTypeName : node.getDefinition().getRawSuperTypes())
            // {
            //     var superNode = typeMap.computeIfAbsent(superTypeName,
            //                                             name -> new TypeNode(name, false));
            //     node.addParent(superNode);
            //     superNode.addChild(node);
            // }
            node.getDefinition().getRawSuperTypes().forEach(superTypeName ->
            {
                var superNode = typeMap.computeIfAbsent(superTypeName,
                                                        name -> new TypeNode(name, false));
                node.addParent(superNode);
                superNode.addChild(node);
            });
        }

        // Connect methods and parameters. (Start a new loop here, because we want all the types
        // added to typeMap in the above loop (if any) to be present.)
        for(var node : new ArrayList<>(typeMap.values()))
        {
            var defn = node.getDefinition();
            if(defn != null)
            {
                // for(var methodDefn : defn.getMethods())
                // {
                //     var method = new MethodNode(methodDefn, node);
                //     node.addMethod(method);
                //     for(var parameterDefn : methodDefn.getParameters())
                //     {
                //         var parameterType = typeMap.computeIfAbsent(parameterDefn.getType(), name -> new TypeNode(name, true));
                //             // (true/false -- we don't actually know whether this type is a class or interface.)
                //
                //         method.addParameter(new ParameterNode(parameterDefn, method, parameterType));
                //     }
                // }
                defn.getMethods().forEach(methodDefn ->
                {
                    if(!methodDefn.isConstructor())
                    {
                        var method = new MethodNode(methodDefn, node);
                        node.addMethod(method);
                        methodDefn.getParameters().forEach(parameterDefn ->
                        {
                            if(!parameterDefn.isImplicit())
                            {
                                // var parameterType = typeMap.computeIfAbsent(
                                    // parameterDefn.getType().orElse(""), // FIXME: Java-specific
                                    // name -> new TypeNode(name, true));
                                    // (true/false -- we don't actually know whether this type is a class or interface.)

                                var paramNode = new ParameterNode(parameterDefn, method);
                                parameterDefn.getType().ifPresent(
                                    name -> paramNode.setType(typeMap.computeIfAbsent(
                                        name,
                                        name_ -> new TypeNode(name_, true))));
                                        // (true/false -- we don't actually know whether this type
                                        // is a class or interface.)
                                method.addParameter(paramNode);
                            }
                        });
                    }
                });
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

    private static void findMethodOverrides(TypeNode type, Map<Signature,MethodNode> superMethods)
    {
        // FIXME: does not account for multiple inheritance.

        var newSuperMethods = new HashMap<>(superMethods);

        for(var method : type.getMethods())
        {
            // var signature = method.getSignature();
            // var superMethod = superMethods.get(signature);
            // if(superMethod != null)
            // {
            //     // var superModifiers = superMethod.getDefinition().getModifiers();
            //     // if(!(superModifiers.contains("private") || superModifiers.contains("static"))) // Can override?
            //     // {
            //     //     method.setOverrides(superMethod);
            //     //     superMethod.addOverriddenBy(method);
            //     // }
            //     // var superDefn = superMethod.getDefinition();
            //     // if(!(superDefn.is(Modifier.PRIVATE) ||
            //     //      superDefn.is(Modifier.STATIC) ||
            //     //      superDefn.is(Modifier.FINAL))) // Can override?
            //     // {
            //     //     method.setOverrides(superMethod);
            //     //     superMethod.addOverriddenBy(method);
            //     // }
            //     method.setOverrides(superMethod);
            //     superMethod.addOverriddenBy(method);
            // }
            //
            // // var modifiers = method.getDefinition().getModifiers();
            // // if(!(modifiers.contains("final") || modifiers.contains("private") || modifiers.contains("static")))
            // // {
            // //     newSuperMethods.put(signature, method);
            // // }
            // var defn = method.getDefinition();
            // if(!(defn.is(Modifier.FINAL) || defn.is(Modifier.PRIVATE) || defn.is(Modifier.STATIC)))
            // {
            //     newSuperMethods.put(signature, method);
            // }

            var defn = method.getDefinition();
            if(!(defn.is(Modifier.PRIVATE) || defn.is(Modifier.STATIC)))
            {
                var signature = method.getSignature();
                var superMethod = superMethods.get(signature);
                if(superMethod != null)
                {
                    method.setOverrides(superMethod);
                    superMethod.addOverriddenBy(method);
                }

                if(!defn.is(Modifier.FINAL))
                {
                    newSuperMethods.put(signature, method);
                }
            }
        }

        for(var child : type.getChildren())
        {
            findMethodOverrides(child, newSuperMethods);
        }
    }

}
