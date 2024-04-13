package au.djac.polytree.tree;
import au.djac.polytree.definitions.*;

import java.util.*;

public class MethodNode implements Comparable<MethodNode>
{
    private final String name;
    private final MethodDefinition defn;
    private final TypeNode type;

    private final ArrayList<ParameterNode> parameters = new ArrayList<>();
    private Signature signature = null;

    private MethodNode overrides = null;
    private final Set<MethodNode> overriddenBy = new HashSet<>();

    public MethodNode(MethodDefinition defn, TypeNode type)
    {
        this.name = defn.getName();
        this.defn = defn;
        this.type = type;
    }

    public MethodNode(String name, TypeNode type)
    {
        this.name = name;
        this.defn = null;
        this.type = type;
    }

    public void addParameter(ParameterNode parameter)
    {
        this.parameters.add(parameter);
    }

    public void setOverrides(MethodNode overrides)
    {
        this.overrides = overrides;
    }

    public void addOverriddenBy(MethodNode overriddenBy)
    {
        this.overriddenBy.add(overriddenBy);
    }

    public String getName()                    { return name; }
    public TypeNode getType()                  { return type; }
    public List<ParameterNode> getParameters() { return Collections.unmodifiableList(parameters); }
    public MethodDefinition getDefinition()    { return defn; }
    public MethodNode getOverrides()           { return overrides; }
    public Set<MethodNode> getOverriddenBy()   { return Collections.unmodifiableSet(overriddenBy); }

    public Signature getSignature()
    {
        if(signature == null)
        {
            signature = new Signature(
                name,
                parameters.stream().map(p -> p.getType().orElse(null)).toList());
        }
        return signature;
    }

    @Override
    public int compareTo(MethodNode other)
    {
        int result = name.compareTo(other.name);
        if(result != 0) { return result; }

        var iterator1 = parameters.iterator();
        var iterator2 = other.parameters.iterator();
        while(iterator1.hasNext() && iterator2.hasNext())
        {
            result = iterator1.next().compareTo(iterator2.next());
            if(result != 0) { return result; }
        }

        if(iterator1.hasNext()) { return 1; }
        if(iterator2.hasNext()) { return -1; }
        return 0;
    }

    @Override
    public boolean equals(Object other)
    {
        if(!(other instanceof MethodNode)) { return false; }
        var otherMethod = (MethodNode)other;
        return type.equals(otherMethod.type) && name.equals(otherMethod.name) && parameters.equals(otherMethod.parameters);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, name, parameters);
    }

    @Override
    public String toString()
    {
        return defn.toString();
    }
}
