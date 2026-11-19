package dev.iris.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Locale;

/** Color setting stored as ARGB int. Serialized as "#AARRGGBB". */
public class ColorSetting extends Setting<Integer> {
    private final boolean allowAlpha;

    public ColorSetting(String name, String description, int argb, boolean allowAlpha) {
        super(name, description, allowAlpha ? argb : (argb | 0xFF000000));
        this.allowAlpha = allowAlpha;
    }

    public ColorSetting(String name, String description, int argb) { this(name, description, argb, false); }

    public boolean allowsAlpha() { return allowAlpha; }
    public int argb() { return value; }

    @Override protected Integer sanitize(Integer candidate) {
        int c = candidate == null ? defaultValue() : candidate;
        return allowAlpha ? c : (c | 0xFF000000);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(hex(value)); }

    @Override public void fromJson(JsonElement element) {
        if (!element.isJsonPrimitive()) throw new IllegalArgumentException("expected color string");
        Integer parsed = parseColor(element.getAsString());
        if (parsed == null) throw new IllegalArgumentException("bad color '" + element.getAsString() + "'");
        set(parsed);
    }

    @Override public String display() { return hex(value); }

    @Override public boolean parse(String text) {
        Integer parsed = parseColor(text);
        if (parsed == null) return false;
        set(parsed);
        return true;
    }

    @Override public List<String> suggestions() {
        return List.of("white", "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "pink", "purple", "gray", "black");
    }

    public static String hex(int argb) { return String.format(Locale.ROOT, "#%08X", argb); }

    /** Accepts #RRGGBB, RRGGBB, #AARRGGBB or a basic color name. Returns null if invalid. */
    public static Integer parseColor(String text) {
        if (text == null) return null;
        String s = text.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "white": return 0xFFFFFFFF;
            case "black": return 0xFF000000;
            case "red": return 0xFFFF5555;
            case "green": return 0xFF55FF55;
            case "blue": return 0xFF5555FF;
            case "yellow": return 0xFFFFFF55;
            case "cyan": return 0xFF55FFFF;
            case "magenta": return 0xFFFF55FF;
            case "orange": return 0xFFFFAA00;
            case "pink": return 0xFFFF79C6;
            case "purple": return 0xFFAA55FF;
            case "gray", "grey": return 0xFFAAAAAA;
            default: break;
        }
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x")) s = s.substring(2);
        if (!s.matches("[0-9a-f]{6}|[0-9a-f]{8}")) return null;
        long v = Long.parseLong(s, 16);
        return s.length() == 6 ? (int) (0xFF000000L | v) : (int) v;
    }

    @Override public ColorSetting visibleWhen(java.util.function.BooleanSupplier supplier) {
        super.visibleWhen(supplier);
        return this;
    }
}
