package dev.iris.client.util;

import java.util.Locale;

/** Text formatting helpers shared by HUD elements, commands and the GUI. */
public final class Format {
    private Format() {}

    public static String fixed(double v, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", v);
    }

    public static String duration(long millis) {
        long s = Math.max(0, millis / 1000);
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h > 0) return String.format(Locale.ROOT, "%dh %02dm %02ds", h, m, sec);
        if (m > 0) return String.format(Locale.ROOT, "%dm %02ds", m, sec);
        return sec + "s";
    }

    public static String bytes(long b) {
        if (b < 1024) return b + " B";
        double kb = b / 1024.0;
        if (kb < 1024) return fixed(kb, 1) + " KB";
        double mb = kb / 1024.0;
        if (mb < 1024) return fixed(mb, 1) + " MB";
        return fixed(mb / 1024.0, 2) + " GB";
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    /** "minecraft:dark_forest" -> "Dark Forest". */
    public static String prettyId(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("[_/]")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(capitalize(part));
        }
        return sb.toString();
    }

    /** Truncates with an ellipsis so the text is at most {@code max} characters. */
    public static String ellipsize(String s, int max) {
        if (s.length() <= max) return s;
        return max <= 1 ? s.substring(0, Math.max(0, max)) : s.substring(0, max - 1) + "…";
    }
}
