package au.djac.polytree.view;
import org.fusesource.jansi.*;

import java.io.*;
import java.util.*;

public class Output
{
    public static final String DEFAULT = "";
    public static final String BRIGHT_WHITE = "1";
    public static final String GREY = "30;1";
    public static final String RED = "31";
    public static final String GREEN = "32";
    public static final String ORANGE = "33";
    public static final String BLUE = "34";
    public static final String MAGENTA = "35";
    public static final String CYAN = "36";
    public static final String BRIGHT_MAGENTA = "35;1";

    public static final char[] UNICODE_CHARSET = {'│', '├', '└', '─', '┊'};
    public static final char[] ASCII_CHARSET   = {'|', '+', '\\', '-', '|'};
    public static final int VERTICAL_CH = 0;
    public static final int INTERSECT_CH = 1;
    public static final int CORNER_CH = 2;
    public static final int HORIZONTAL_CH = 3;
    public static final int VERTICAL_DOTTED_CH = 4;

    static {
        AnsiConsole.systemInstall();
    }

    private char[] charSet = UNICODE_CHARSET;
    private int startCol = 0;
    private int column = 0;
    private int terminalWidth;
    private PrintStream out;

    public static Output withAnsi()
    {
        var out = AnsiConsole.out();
        out.setMode(AnsiMode.Force);
        return new Output(out);
    }

    public Output(PrintStream out)
    {
        terminalWidth = AnsiConsole.getTerminalWidth();
        if(terminalWidth < 1)
        {
            terminalWidth = 80;
        }
        this.out = out;
    }

    public void startCount()
    {
        startCol = column;
    }

    public int getNChars()
    {
        return column - startCol;
    }

    public Output ascii(boolean ascii)
    {
        charSet = ascii ? ASCII_CHARSET : UNICODE_CHARSET;
        return this;
    }

    public String chars(int... indexes)
    {
        var chars = new char[indexes.length];
        for(int i = 0; i < indexes.length; i++)
        {
            chars[i] = charSet[indexes[i]];
        }
        return new String(chars);
    }

    public void print(String s)
    {
        column += s.length();
        out.print(s);
    }

    public void print(String s, String colourCode)
    {
        column += s.length();
        out.printf("\033[%sm%s\033[m", colourCode, s);
    }

    public void newLine()
    {
        column = 0;
        out.println();
    }

    public void println(String s)
    {
        print(s);
        newLine();
    }

    public void println(String s, String colourCode)
    {
        print(s, colourCode);
        newLine();
    }

    public void printRight(String s, String colourCode)
    {
        out.print(" ".repeat(Math.max(0, terminalWidth - (column + s.length()))));
        println(s, colourCode);
    }

    public void printJoin(String separator, String separatorColourCode, List<String> values, String valueColourCode)
    {
        boolean first = true;
        for(var s : values)
        {
            if(!first)
            {
                print(separator, separatorColourCode);
            }
            first = false;
            print(s, valueColourCode);
        }
    }
}
