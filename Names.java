package dev.iris.client.util;

import java.util.regex.Pattern;

/** Validation for user-chosen names (configs, themes). Blocks path separators and odd characters. */
public final class Names {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 _\\-]{0,31}");

    private Names() {}

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches() && !name.endsWith(" ");
    }

    public static String requirements() {
        return "1-32 characters: letters, digits, spaces, '-' or '_' (must start with a letter or digit)";
    }

    /** File-system safe file name stem for a validated name. */
    public static String fileStem(String name) { return name.trim().replace(' ', '_'); }
}
