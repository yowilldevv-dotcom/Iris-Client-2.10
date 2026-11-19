package dev.iris.client.gui;

import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.theme.Theme;
import dev.iris.client.util.Input;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

/** Single-line text input with cursor, selection-free editing, paste support and an optional character filter. */
public final class TextField {
    private final StringBuilder text = new StringBuilder();
    private int cursor;
    private boolean focused;
    private final int maxLength;
    private Predicate<Integer> filter = cp -> cp >= 32 && cp != 127;
    private final String placeholder;

    public TextField(String placeholder, int maxLength) {
        this.placeholder = placeholder;
        this.maxLength = maxLength;
    }

    public TextField filter(Predicate<Integer> codePointFilter) {
        this.filter = codePointFilter;
        return this;
    }

    public String text() { return text.toString(); }

    public void setText(String s) {
        text.setLength(0);
        text.append(s == null ? "" : s, 0, Math.min(maxLength, s == null ? 0 : s.length()));
        cursor = text.length();
    }

    public boolean isFocused() { return focused; }
    public void setFocused(boolean f) { focused = f; }
    public void clear() { setText(""); }

    public void draw(Draw d, float x, float y, float w, float h) {
        Theme t = Ui.theme();
        d.rrect(x, y, w, h, 3, t.panelAlt());
        if (focused) d.outline(x, y, w, h, Colors.mulAlpha(t.accent(), 0.9f));
        float ty = y + (h - d.lineHeight()) / 2f + 1;
        String shown = text.toString();
        if (shown.isEmpty() && !focused) {
            d.textFit(placeholder, x + 4, ty, w - 8, t.textDim(), false);
            return;
        }
        // Scroll so the cursor stays visible.
        String before = shown.substring(0, cursor);
        float offset = Math.max(0, d.width(before) - (w - 10));
        d.scissor(x + 2, y, w - 4, h);
        d.text(shown, x + 4 - offset, ty, t.text(), false);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            d.rect(x + 4 + d.width(before) - offset, ty - 1, 1, d.lineHeight() + 1, t.text());
        }
        d.endScissor();
    }

    public void onChar(int cp) {
        if (!focused || !filter.test(cp) || text.length() >= maxLength) return;
        String s = new String(Character.toChars(cp));
        text.insert(cursor, s);
        cursor += s.length();
    }

    /** Returns true if the key was consumed. */
    public boolean onKey(int key, int modifiers) {
        if (!focused) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0) { text.deleteCharAt(cursor - 1); cursor--; }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < text.length()) text.deleteCharAt(cursor);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> { cursor = Math.max(0, cursor - 1); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { cursor = Math.min(text.length(), cursor + 1); return true; }
            case GLFW.GLFW_KEY_HOME -> { cursor = 0; return true; }
            case GLFW.GLFW_KEY_END -> { cursor = text.length(); return true; }
            case GLFW.GLFW_KEY_V -> {
                if (!ctrl) return false;
                for (int cp : Input.clipboard().codePoints().toArray()) onChar(cp);
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (!ctrl) return false;
                cursor = text.length();
                return true;
            }
            case GLFW.GLFW_KEY_C -> {
                if (!ctrl) return false;
                Input.setClipboard(text.toString());
                return true;
            }
            default -> { return false; }
        }
    }
}
