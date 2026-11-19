package dev.iris.client.setting;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Maps GLFW key codes to friendly names and back. -1 means "unbound". */
public final class KeyNames {
    public static final int NONE = -1;
    private static final Map<String, Integer> BY_NAME = new LinkedHashMap<>();
    private static final Map<Integer, String> BY_CODE = new LinkedHashMap<>();

    static {
        for (char c = 'A'; c <= 'Z'; c++) put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'A'));
        for (char c = '0'; c <= '9'; c++) put(String.valueOf(c), GLFW.GLFW_KEY_0 + (c - '0'));
        for (int i = 1; i <= 12; i++) put("F" + i, GLFW.GLFW_KEY_F1 + (i - 1));
        for (int i = 0; i <= 9; i++) put("KP" + i, GLFW.GLFW_KEY_KP_0 + i);
        put("SPACE", GLFW.GLFW_KEY_SPACE);
        put("TAB", GLFW.GLFW_KEY_TAB);
        put("ENTER", GLFW.GLFW_KEY_ENTER);
        put("BACKSPACE", GLFW.GLFW_KEY_BACKSPACE);
        put("INSERT", GLFW.GLFW_KEY_INSERT);
        put("DELETE", GLFW.GLFW_KEY_DELETE);
        put("HOME", GLFW.GLFW_KEY_HOME);
        put("END", GLFW.GLFW_KEY_END);
        put("PAGEUP", GLFW.GLFW_KEY_PAGE_UP);
        put("PAGEDOWN", GLFW.GLFW_KEY_PAGE_DOWN);
        put("UP", GLFW.GLFW_KEY_UP);
        put("DOWN", GLFW.GLFW_KEY_DOWN);
        put("LEFT", GLFW.GLFW_KEY_LEFT);
        put("RIGHT", GLFW.GLFW_KEY_RIGHT);
        put("LSHIFT", GLFW.GLFW_KEY_LEFT_SHIFT);
        put("RSHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT);
        put("LCTRL", GLFW.GLFW_KEY_LEFT_CONTROL);
        put("RCTRL", GLFW.GLFW_KEY_RIGHT_CONTROL);
        put("LALT", GLFW.GLFW_KEY_LEFT_ALT);
        put("RALT", GLFW.GLFW_KEY_RIGHT_ALT);
        put("GRAVE", GLFW.GLFW_KEY_GRAVE_ACCENT);
        put("MINUS", GLFW.GLFW_KEY_MINUS);
        put("EQUALS", GLFW.GLFW_KEY_EQUAL);
        put("LBRACKET", GLFW.GLFW_KEY_LEFT_BRACKET);
        put("RBRACKET", GLFW.GLFW_KEY_RIGHT_BRACKET);
        put("SEMICOLON", GLFW.GLFW_KEY_SEMICOLON);
        put("APOSTROPHE", GLFW.GLFW_KEY_APOSTROPHE);
        put("COMMA", GLFW.GLFW_KEY_COMMA);
        put("PERIOD", GLFW.GLFW_KEY_PERIOD);
        put("SLASH", GLFW.GLFW_KEY_SLASH);
        put("BACKSLASH", GLFW.GLFW_KEY_BACKSLASH);
    }

    private KeyNames() {}

    private static void put(String name, int code) {
        BY_NAME.put(name, code);
        BY_CODE.put(code, name);
    }

    public static String name(int code) { return code < 0 ? "None" : BY_CODE.getOrDefault(code, "KEY" + code); }

    /** Returns the key code, {@link #NONE} for "none", or Integer.MIN_VALUE if the name is unknown. */
    public static int parse(String text) {
        String s = text.trim().toUpperCase(Locale.ROOT);
        if (s.equals("NONE") || s.equals("UNBOUND") || s.isEmpty()) return NONE;
        Integer code = BY_NAME.get(s);
        if (code != null) return code;
        if (s.startsWith("KEY")) {
            try { return Integer.parseInt(s.substring(3)); } catch (NumberFormatException ignored) { /* fall through */ }
        }
        return Integer.MIN_VALUE;
    }

    public static List<String> all() { return new ArrayList<>(BY_NAME.keySet()); }
}
