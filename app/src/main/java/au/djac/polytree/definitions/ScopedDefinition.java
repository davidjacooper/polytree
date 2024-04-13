package au.djac.polytree.definitions;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

public abstract class ScopedDefinition extends Definition
{
    private boolean descendable = false;
    private boolean ascendable = false;
    private Optional<ScopedDefinition> containing = Optional.empty();
    private Optional<String> typeParams           = Optional.empty();
    private final Map<String,ScopedDefinition> nested = new LinkedHashMap<>();

    private final List<Import.Supplier> importSuppliers = new ArrayList<>();
    private List<Import> imports = null;

    public ScopedDefinition(SourceFile file, int startPos, int endPos, String name)
    {
        super(file, startPos, endPos, name);
    }

    public ScopedDefinition(String name)
    {
        super(name);
    }

    public void setAscendable(boolean ascendable) { this.ascendable = ascendable; }
    public void setDescendable(boolean descendable) { this.descendable = descendable; }

    public void setTypeParams(String typeParams)
    {
        this.typeParams = Optional.of(typeParams);
    }

    private void validateNested(ScopedDefinition defn)
    {
        if(getSourceFile().isPresent() &&
            (defn.getStartPos() < getStartPos() || defn.getEndPos() > getEndPos()))
        {
            throw new IllegalArgumentException(
                String.format(
                    "Definition %s (%d-%d) cannot be nested within %s (%d-%d)",
                    defn, defn.getStartPos(), defn.getEndPos(),
                    this, getStartPos(), getEndPos()));
        }
    }

    public void addNested(ScopedDefinition defn)
    {
        validateNested(defn);
        nested.put(defn.getName(), defn);
        defn.containing = Optional.of(this);
    }

    public void addImportSupplier(Import.Supplier supplier)
    {
        importSuppliers.add(supplier);
    }

    public void addImportSuppliers(Collection<Import.Supplier> supplier)
    {
        importSuppliers.addAll(supplier);
    }

    public boolean isDescendable()                    { return descendable; }
    public boolean isAscendable()                     { return ascendable; }
    public Optional<ScopedDefinition> getContaining() { return containing; }
    public Optional<String> getTypeParams()           { return typeParams; }
    public Stream<ScopedDefinition> getNested()       { return nested.values().stream(); }
    public Stream<ScopedDefinition> getNamedScopes()  { return Stream.of(this); }

    public Stream<Import> getImports()
    {
        if(imports == null)
        {
            imports = importSuppliers.stream().flatMap(Import.Supplier::get).toList();
        }
        return imports.stream();
    }

    public Optional<ScopedDefinition> getNested(String name)
    {
        return Optional.ofNullable(nested.get(name));
    }

    public ScopedDefinition getOrAddNested(String name, Supplier<? extends ScopedDefinition> supplier)
    {
        return nested.computeIfAbsent(name, k ->
        {
            var defn = supplier.get();
            validateNested(defn);
            defn.containing = Optional.of(this);
            return defn;
        });
    }

    public Stream<ScopedDefinition> walk()
    {
        return Stream.concat(
            Stream.of(this),
            nested.values().stream().flatMap(ScopedDefinition::walk)
        );
    }

    public <D extends ScopedDefinition> Stream<D> walk(Class<D> defnClass)
    {
        var stream = walk().filter(d -> defnClass.isAssignableFrom(d.getClass()));

        @SuppressWarnings("unchecked")
        var castStream = (Stream<D>)stream;
        return castStream;
    }

    public <D extends ScopedDefinition> Stream<D> resolve(Class<D> defnClass, List<String> names)
    {
        return Stream.<Supplier<Stream<D>>>of(
            () -> resolveLocally(defnClass, names),
            () -> resolveFromImports(defnClass, names),
            () -> ascendable
                ? containing.stream().flatMap(superScope -> superScope.resolve(defnClass, names))
                : Stream.empty())
            .flatMap(Supplier::get);
    }

    private <D extends ScopedDefinition> Stream<D> resolveFromImports(Class<D> defnClass,
                                                                      List<String> names)
    {
        return getImports().flatMap(import_ ->
        {
            var importNames = import_.getLocalName();
            var importNameSize = importNames.size();
            var thisNameSize = names.size();

            if(!(importNameSize <= thisNameSize &&
                names.subList(0, importNameSize).equals(importNames)))
            {
                return Stream.empty();
            }

            var importedDefn = import_.getSource();

            if(importNameSize == thisNameSize)
            {
                if(defnClass.isAssignableFrom(importedDefn.getClass()))
                {
                    @SuppressWarnings("unchecked")
                    var importedDefnD = (D) importedDefn;

                    return Stream.of(importedDefnD);
                }
                else
                {
                    return Stream.empty();
                }
            }

            return importedDefn.resolveLocally(defnClass,
                                               names.subList(importNameSize, thisNameSize));
         });
    }

    public <D extends ScopedDefinition> Stream<D> resolveLocally(Class<D> defnClass, List<String> names)
    {
        return nested.values().stream()
            .flatMap(subScope -> subScope.resolveHere(defnClass, names));
    }

    public <D extends ScopedDefinition> Stream<D> resolveHere(Class<D> defnClass,
                                                              List<String> names)
    {
        var size = names.size();
        if(size == 0) { throw new IllegalArgumentException("Name list cannot be empty"); }

        var topName = names.get(0);

        if(!topName.equals(getName()))
        {
            return Stream.empty();
        }

        if(size == 1)
        {
            if(defnClass.isAssignableFrom(getClass()))
            {
                @SuppressWarnings("unchecked")
                var d = (D)this;
                return Stream.of(d);
            }
            else
            {
                return Stream.empty();
            }
        }

        return nested.values().stream()
            .filter(ScopedDefinition::isDescendable)
            .flatMap(subScope -> subScope.resolveHere(defnClass, names.subList(1, size)));
    }
}
