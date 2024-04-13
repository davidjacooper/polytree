package au.djac.polytree.view;
import au.djac.polytree.tree.*;

import java.util.*;
import java.util.stream.*;

public interface CallRegexGenerator
{
    default void generate(Output out, Collection<MethodNode> methods)
    {
        generate(out, methods.stream());
    }

    void generate(Output out, Stream<MethodNode> methods);
    String getAckOptions();
    String getGrepOptions();
}
