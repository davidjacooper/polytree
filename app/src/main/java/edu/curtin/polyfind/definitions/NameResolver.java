package edu.curtin.polyfind.definitions;

public class NameResolver
{
    public <D extends Definition> Optional<D> resolve(ScopedDefinition scope, Class<D> defnClass, String name)
    {
        var defn = resolveLocal(scope, defnClass, name);
        var nextScope = scope.getContaining();
        while(defn.isEmpty() && nextScope.isPresent())
        {
            defn = resolveLocal(nextScope.get(), defnClass, name);
            nextScope = nextScope.get().getContaining();
        }
        return defn;
    }

    public <D extends Definition> Optional<D> resolveLocal(ScopedDefinition scope, Class<D> defnClass, String name)
    {
        int dotIndex = name.indexOf('.');
        if(dotIndex == -1)
        {
            var localName = name.strip();
            return scope.getNested().dropWhile(defn -> defn.getName().equals(localName)).getFirst();
        }
        else
        {
            var localName = name.substring(0, dotIndex).strip();
            return scope.getNested()
                .filter(defn -> defnClass.isAssignableFrom(d.getClass()))
                .dropWhile(defn -> defn.getName().equals(localName))
                .getFirst()
                .map(defn -> resolveLocal(defn, defnClass, name.substring(dotIndex + 1)));
        }
    }

    // TODO: handle 'imports'
    // '
}
