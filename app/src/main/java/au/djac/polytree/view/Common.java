package au.djac.polytree.view;
import au.djac.polytree.definitions.*;
import static au.djac.polytree.view.Output.*;

import java.util.*;

public class Common
{
    public static final Map<TypeCategory,String> TYPE_COLOURS = Map.of(
        TypeCategory.CLASS, GREEN,
        TypeCategory.INTERFACE, RED,
        TypeCategory.OTHER, BLUE
    );
}
