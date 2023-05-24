package edu.curtin.polyfind.languages;
import edu.curtin.polyfind.parsing.*;
import edu.curtin.polyfind.view.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class LanguageSet
{
    private static Map<String,Supplier<Language>> LANGUAGE_CONSTRUCTORS = Map.of(
        "java", () -> new Language("Java",
                                   new JavaParser(),
                                   new JavaCallRegexGenerator()),
        "py",   () -> new Language("Python",
                                   new PythonParser(),
                                   new PythonCallRegexGenerator())
    );

    private Map<String,Language> languages = new HashMap<>();

    public Optional<Language> getByPath(Path path)
    {
        var name = path.toFile().getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');
        if(dotIndex == -1)
        {
            return Optional.empty();
        }
        return getByExtension(name.substring(dotIndex + 1));
    }

    public Optional<Language> getByExtension(String extension)
    {
        var lang = languages.get(extension);
        if(lang == null)
        {
            var supplier = LANGUAGE_CONSTRUCTORS.get(extension);
            if(supplier == null)
            {
                return Optional.empty();
            }
            lang = supplier.get();
            languages.put(extension, lang);
        }
        return Optional.of(lang);
    }

    public Stream<Language> getAll()
    {
        return languages.values().stream();
    }
}
