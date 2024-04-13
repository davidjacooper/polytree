package au.djac.polytree.definitions;

public enum TypeCategory
{
    CLASS("class"), INTERFACE("interface"), OTHER("type");

    public final String genericName;

    TypeCategory(String genericName)
    {
        this.genericName = genericName;
    }
}
