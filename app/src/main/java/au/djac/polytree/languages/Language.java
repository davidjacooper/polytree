package au.djac.polytree.languages;
import au.djac.polytree.definitions.*;
import au.djac.polytree.parsing.*;
import au.djac.polytree.view.*;

public class Language
{
    private final String name;
    private final Parser parser;
    private final CallRegexGenerator callRegexGenerator;

    public Language(String name, Parser parser,
                    CallRegexGenerator callRegexGenerator)
    {
        this.name = name;
        this.parser = parser;
        this.callRegexGenerator = callRegexGenerator;
    }

    public String getName() { return name; }
    public Parser getParser() { return parser; }
    public CallRegexGenerator getCallRegexGenerator() { return callRegexGenerator; }

    @Override
    public String toString()
    {
        return name;
    }
}
