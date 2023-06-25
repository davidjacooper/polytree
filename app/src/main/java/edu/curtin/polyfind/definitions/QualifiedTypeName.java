package edu.curtin.polyfind.definitions;

import java.util.*;

public class QualifiedTypeName extends QualifiedName
{
    private Optional<String> constructHint = Optional.empty();
    private Optional<TypeCategory> categoryHint = Optional.empty();

    public QualifiedTypeName(ScopedDefinition scope, List<String> names, String displayName, boolean withinScope)
    {
        super(scope, names, displayName, withinScope);
    }

    public QualifiedTypeName categoryHint(TypeCategory categoryHint)
    {
        this.categoryHint = Optional.of(categoryHint);
        return this;
    }

    public QualifiedTypeName constructHint(String constructHint)
    {
        this.constructHint = Optional.of(constructHint);
        return this;
    }

    public TypeCategory getCategoryHint()
    {
        return categoryHint.orElse(TypeCategory.OTHER);
    }

    public String getConstructHint()
    {
        return constructHint.orElseGet(() -> getCategoryHint().genericName);
    }

    public Optional<TypeDefinition> resolve()
    {
        return resolve(TypeDefinition.class).findFirst();
    }
}
