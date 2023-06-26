package edu.curtin.polyfind.view;
import edu.curtin.polyfind.definitions.*;
import static edu.curtin.polyfind.view.Output.*;

import java.util.*;

public class Common
{
    public static final Map<TypeCategory,String> TYPE_COLOURS = Map.of(
        TypeCategory.CLASS, GREEN,
        TypeCategory.INTERFACE, RED,
        TypeCategory.OTHER, BLUE
    );
}
