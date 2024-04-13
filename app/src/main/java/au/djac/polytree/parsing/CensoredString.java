package au.djac.polytree.parsing;

import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class CensoredString
{
    private static final char DEFAULT_CENSOR_CHAR = '\032'; // ASCII "substitute" control character

    private final String uncensored;
    private final StringBuilder censored;

    public CensoredString(String uncensored, String censored)
    {
        this.uncensored = uncensored;
        this.censored = new StringBuilder(censored);
    }

    public CensoredString(String s)
    {
        this(s, s);
    }

    public int length()
    {
        return uncensored.length();
    }

    public String uncensored()
    {
        return uncensored;
    }

    public String censored()
    {
        return censored.toString();
    }

    public void censorIteratively(Pattern p) { censorIteratively(p, DEFAULT_CENSOR_CHAR); }
    public boolean censor(Pattern p)         { return censor    (p, DEFAULT_CENSOR_CHAR); }
    public void censor(Matcher m)            { censor           (m, DEFAULT_CENSOR_CHAR); }
    public void censor(int s, int e)         { censor           (s, e, DEFAULT_CENSOR_CHAR); }


    public void censorIteratively(Pattern pattern, char censorChar)
    {
        while(censor(pattern, censorChar)) {}
    }

    public boolean censor(Pattern pattern, char censorChar)
    {
        var matcher = pattern.matcher(censored);
        boolean found = false;
        while(matcher.find())
        {
            censor(matcher.start(), matcher.end(), censorChar);
            found = true;
        }
        return found;
    }

    public void censor(Matcher matcher, char censorChar)
    {
        censor(matcher.start(), matcher.end(), censorChar);
    }

    public void censor(int start, int end, char censorChar)
    {
        for(int i = start; i < end; i++)
        {
            if(censored.charAt(i) > ' ')
            {
                censored.setCharAt(i, censorChar);
            }
        }
    }

    public Matcher matcher(Pattern pattern)
    {
        return new Matcher(pattern.matcher(censored));
    }

    public class Matcher
    {
        private java.util.regex.Matcher matcher;

        Matcher(java.util.regex.Matcher matcher)
        {
            this.matcher = matcher;
        }

        public boolean find()
        {
            return matcher.find();
        }

        public int start()
        {
            return matcher.start();
        }

        public int end()
        {
            return matcher.end();
        }

        public int start(String name)
        {
            return matcher.start(name);
        }

        public int end(String name)
        {
            return matcher.end(name);
        }

        public String uncensored()
        {
            return uncensored.substring(matcher.start(), matcher.end());
        }

        public String censored()
        {
            return matcher.group();
        }

        public Optional<String> uncensoredGroup(String name)
        {
            int start = matcher.start(name);
            if(start == -1) { return Optional.empty(); }

            return Optional.of(uncensored.substring(start, matcher.end(name)));
        }

        public Optional<CensoredString> censoredGroup(String name)
        {
            String censoredGroup = matcher.group(name);
            if(censoredGroup == null) { return Optional.empty(); }

            return Optional.of(new CensoredString(
                uncensored.substring(matcher.start(name), matcher.end(name)),
                censoredGroup));
        }

        public boolean hasGroup(String name)
        {
            return matcher.group(name) != null;
        }

        public Stream<String> resultsUncensored()
        {
            return matcher.results().map(m -> uncensored.substring(m.start(), m.end()));
        }
    }
}
