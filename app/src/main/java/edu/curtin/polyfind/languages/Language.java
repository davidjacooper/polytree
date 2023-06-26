package edu.curtin.polyfind.languages;
import edu.curtin.polyfind.definitions.*;
import edu.curtin.polyfind.parsing.*;
import edu.curtin.polyfind.view.*;

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
