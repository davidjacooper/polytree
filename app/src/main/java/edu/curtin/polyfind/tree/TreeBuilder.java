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
            node.getDefinition().ifPresent(d ->
                d.getRawSuperTypes().forEach(superTypeName ->
                {
                    var superNode = typeMap.computeIfAbsent(superTypeName,
                                                            name -> new TypeNode(name, false));
                    node.addParent(superNode);
                    superNode.addChild(node);
                })
            );
        }

        // Connect methods and parameters. (Start a new loop here, because we want all the types
        // added to typeMap in the above loop (if any) to be present.)
        for(var node : new ArrayList<>(typeMap.values()))
        {
            node.getDefinition().ifPresent(defn ->
            {
                defn.getMethods()
                    .filter(d -> !d.isConstructor())
                    .forEach(methodDefn ->
                {
                    var method = new MethodNode(methodDefn, node);
                    node.addMethod(method);
                    methodDefn.getParameters()
                              .filter(d -> !d.isImplicit())
                              .forEach(parameterDefn ->
                    {
                        var paramNode = new ParameterNode(parameterDefn, method);
                        parameterDefn.getType().ifPresent(
                            name -> paramNode.setType(typeMap.computeIfAbsent(
                                name,
                                name_ -> new TypeNode(name_, true))));
                                // (true/false -- we don't actually know whether this type
                                // is a class or interface.)
                        method.addParameter(paramNode);
                    });
                });
            });
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
