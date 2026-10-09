package betteradvancements.common.util;

import java.util.Arrays;
import java.util.List;

public enum TabSortMode {
    ORIGINAL,
    ALPHABETICAL,
    COMPLETION,
    CUSTOM;

    public static TabSortMode fromName(String value) {
        if (value != null) {
            for (TabSortMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value)) {
                    return mode;
                }
            }
        }
        return ORIGINAL;
    }

    public static List<TabSortMode> valuesAsList() {
        return Arrays.asList(values());
    }
}
