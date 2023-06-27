package edu.curtin.polyfind.tree;
import edu.curtin.polyfind.definitions.*;

import java.util.*;

public class TreeBuilder
{
    private Map<TypeDefinition,TypeNode> typeMap = new HashMap<>();
    private Map<String,TypeNode> externalTypeMap = new HashMap<>();

    public TreeBuilder() {}

    public void addDefinition(TypeDefinition defn)
    {
        if(typeMap.containsKey(defn))
        {
            throw new IllegalArgumentException("Definition already added");
        }
        typeMap.put(defn, new ProjectTypeNode(defn));
    }

    public Collection<TypeNode> build()
    {
        // Connect type nodes by inheritance.
        for(var node : new ArrayList<>(typeMap.values()))
        {
            node.getDefinition()
                .stream()
                .flatMap(TypeDefinition::getSuperTypes)
                .map(this::getTypeNode)
                .forEach(superNode ->
                {
                    node.addParent(superNode);
                    superNode.addChild(node);
                });
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
                            name -> paramNode.setType(getTypeNode(name)));
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

        var allTypeNodes = new ArrayList<>(typeMap.values());
        allTypeNodes.addAll(externalTypeMap.values());

        return allTypeNodes;
    }

    private TypeNode getTypeNode(QualifiedTypeName name)
    {
        return name
            .resolve(TypeDefinition.class)
            .findFirst()
            .map(superDefn -> typeMap.computeIfAbsent(superDefn, ProjectTypeNode::new))
            .orElseGet(() ->
                externalTypeMap.computeIfAbsent(
                    name.toString(),
                    nameStr -> new ExternalTypeNode(nameStr,
                                                    name.getCategoryHint(),
                                                    name.getConstructHint())
                )
            );
    }

    private static void findMethodOverrides(TypeNode type, Map<Signature,MethodNode> superMethods)
    {
        // FIXME: does not account for multiple inheritance.

        var newSuperMethods = new HashMap<>(superMethods);

        for(var method : type.getMethods())
        {
            var defn = method.getDefinition();
            if(!defn.hasAnyModifier(Modifier.PRIVATE, Modifier.STATIC))
            {
                var signature = method.getSignature();
                var superMethod = superMethods.get(signature);
                if(superMethod != null)
                {
                    method.setOverrides(superMethod);
                    superMethod.addOverriddenBy(method);
                }

                if(!defn.hasModifier(Modifier.FINAL))
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
