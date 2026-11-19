package dev.iris.client.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.iris.client.setting.ColorSetting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named palette of color tokens. All Iris GUI code reads colors from the current theme every frame, so switching
 * or editing a theme updates the whole interface immediately.
 */
public final class Theme {
    /** Token names, in the order they are shown in the theme editor. */
    public static final List<String> TOKENS = List.of(
            "background", "panel", "panelAlt", "header", "accent", "accent2",
            "text", "textDim", "outline", "success", "warning", "error");

    private String name;
    private final boolean builtin;
    private final Map<String, Integer> colors = new LinkedHashMap<>();

    public Theme(String name, boolean builtin, int background, int panel, int panelAlt, int header, int accent,
                 int accent2, int text, int textDim, int outline, int success, int warning, int error) {
        this.name = name;
        this.builtin = builtin;
        int[] values = {background, panel, panelAlt, header, accent, accent2, text, textDim, outline, success, warning, error};
        for (int i = 0; i < TOKENS.size(); i++) colors.put(TOKENS.get(i), values[i]);
    }

    private Theme(String name, boolean builtin, Map<String, Integer> source) {
        this.name = name;
        this.builtin = builtin;
        this.colors.putAll(source);
    }

    public String name() { return name; }
    public boolean isBuiltin() { return builtin; }

    void rename(String newName) { this.name = newName; }

    public int get(String token) { return colors.getOrDefault(token, 0xFFFF00FF); }

    /** Sets a token on a custom theme. Returns false for built-in themes or unknown tokens. */
    public boolean set(String token, int argb) {
        if (builtin || !TOKENS.contains(token)) return false;
        colors.put(token, argb);
        return true;
    }

    public int background() { return get("background"); }
    public int panel() { return get("panel"); }
    public int panelAlt() { return get("panelAlt"); }
    public int header() { return get("header"); }
    public int accent() { return get("accent"); }
    public int accent2() { return get("accent2"); }
    public int text() { return get("text"); }
    public int textDim() { return get("textDim"); }
    public int outline() { return get("outline"); }
    public int success() { return get("success"); }
    public int warning() { return get("warning"); }
    public int error() { return get("error"); }

    /** Editable copy under a new name. */
    public Theme copyAs(String newName) { return new Theme(newName, false, colors); }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        JsonObject c = new JsonObject();
        for (String t : TOKENS) c.addProperty(t, ColorSetting.hex(colors.get(t)));
        o.add("colors", c);
        return o;
    }

    /**
     * Parses a custom theme. Missing tokens fall back to {@code fallback}. Throws IllegalArgumentException for
     * structurally invalid input.
     */
    public static Theme fromJson(JsonObject o, Theme fallback) {
        JsonElement n = o.get("name");
        if (n == null || !n.isJsonPrimitive()) throw new IllegalArgumentException("theme has no name");
        JsonElement c = o.get("colors");
        if (c == null || !c.isJsonObject()) throw new IllegalArgumentException("theme has no colors");
        Theme t = fallback.copyAs(n.getAsString());
        JsonObject colorObj = c.getAsJsonObject();
        for (String token : TOKENS) {
            JsonElement e = colorObj.get(token);
            if (e == null || !e.isJsonPrimitive()) continue;
            Integer parsed = ColorSetting.parseColor(e.getAsString());
            if (parsed == null) throw new IllegalArgumentException("bad color for '" + token + "'");
            t.colors.put(token, parsed);
        }
        return t;
    }
}
