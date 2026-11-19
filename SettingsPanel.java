package dev.iris.client.gui;

import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ColorSetting;
import dev.iris.client.setting.KeybindSetting;
import dev.iris.client.setting.ModeSetting;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.setting.Setting;
import dev.iris.client.setting.SettingGroup;
import dev.iris.client.setting.TextSetting;
import dev.iris.client.theme.Theme;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders and edits every setting of one {@link SettingGroup}: toggles, sliders, dropdowns, colour pickers,
 * text inputs and keybind capture. Call {@link #layout} every frame before {@link #draw}.
 */
public final class SettingsPanel {
    private static final float ROW = 14f;
    private static final float NUMBER_ROW = 21f;
    private static final float OPTION = 12f;
    private static final float CHANNEL = 11f;

    private static final class Row {
        Setting<?> setting;
        float y;
        float h;
    }

    private final SettingGroup group;
    private final List<Row> rows = new ArrayList<>();
    private final TextField field = new TextField("", 64);
    private float x, y, w;

    private Setting<?> openDropdown;
    private Setting<?> openColor;
    private Setting<?> keyCapture;
    private Setting<?> textEdit;
    private NumberSetting draggingNumber;
    private ColorSetting draggingColor;
    private int draggingChannel;

    public SettingsPanel(SettingGroup group) { this.group = group; }

    public SettingGroup group() { return group; }

    private static float baseHeight(Setting<?> s) { return s instanceof NumberSetting ? NUMBER_ROW : ROW; }

    /** Positions all controls and returns the total height. */
    public float layout(float px, float py, float width) {
        x = px;
        y = py;
        w = width;
        rows.clear();
        float cy = py;
        for (Setting<?> s : group.settings()) {
            if (!s.isVisible()) continue;
            Row r = new Row();
            r.setting = s;
            r.y = cy;
            r.h = baseHeight(s);
            if (s == openDropdown && s instanceof ModeSetting m) r.h += m.options().size() * OPTION + 2;
            if (s == openColor && s instanceof ColorSetting) r.h += 3 * CHANNEL + 2;
            rows.add(r);
            cy += r.h + 1;
        }
        return cy - py;
    }

    // ---- drawing ----

    public void draw(Draw d, float mx, float my) {
        Theme t = Ui.theme();
        for (Row r : rows) {
            Setting<?> s = r.setting;
            boolean over = Ui.hover(mx, my, x, r.y, w, baseHeight(s));
            if (over) d.rect(x, r.y, w, baseHeight(s), Colors.mulAlpha(t.accent(), 0.08f));
            float ty = r.y + 3;
            float valueW = valueWidth(d, s);
            d.textFit(s.name(), x + 4, ty, Math.max(10, w - valueW - 12), t.text(), false);

            if (s instanceof BoolSetting b) {
                Ui.toggle(d, x + w - 26, r.y + 2, b.on(), "set:" + group.id() + ":" + s.key());
            } else if (s instanceof NumberSetting n) {
                d.textRight(n.display(), x + w - 4, ty, t.accent(), false);
                Ui.slider(d, x + 4, r.y + 12, w - 8, 8, n.fraction());
            } else if (s instanceof ModeSetting m) {
                d.textRight(m.display() + (s == openDropdown ? " ^" : " v"), x + w - 4, ty, t.accent(), false);
                if (s == openDropdown) {
                    float oy = r.y + ROW + 1;
                    d.rrect(x + 4, oy, w - 8, m.options().size() * OPTION, 3, t.panelAlt());
                    for (String o : m.options()) {
                        boolean sel = o.equals(m.get());
                        boolean oOver = Ui.hover(mx, my, x + 4, oy, w - 8, OPTION);
                        if (oOver) d.rect(x + 5, oy, w - 10, OPTION, Colors.mulAlpha(t.accent(), 0.25f));
                        d.text(o, x + 9, oy + 2, sel ? t.accent() : t.text(), false);
                        oy += OPTION;
                    }
                }
            } else if (s instanceof ColorSetting c) {
                if (s == textEdit) {
                    field.draw(d, x + w - 70, r.y + 1, 66, ROW - 2);
                } else {
                    d.textRight(c.display(), x + w - 20, ty, t.textDim(), false);
                }
                d.rect(x + w - 16, r.y + 3, 12, 8, c.argb());
                d.outline(x + w - 16, r.y + 3, 12, 8, t.outline());
                if (s == openColor) drawChannels(d, c, r.y + ROW + 1);
            } else if (s instanceof TextSetting) {
                if (s == textEdit) field.draw(d, x + w * 0.4f, r.y + 1, w * 0.6f - 4, ROW - 2);
                else d.textRight(s.display(), x + w - 4, ty, t.accent(), false);
            } else if (s instanceof KeybindSetting) {
                d.textRight(s == keyCapture ? "press a key..." : s.display(), x + w - 4, ty, s == keyCapture ? t.warning() : t.accent(), false);
            }
        }
    }

    private float valueWidth(Draw d, Setting<?> s) {
        if (s instanceof BoolSetting) return 26;
        if (s instanceof ColorSetting) return 90;
        if (s instanceof TextSetting) return Math.min(w * 0.6f, d.width(s.display()));
        return Math.min(w * 0.55f, d.width(s.display()) + 14);
    }

    private void drawChannels(Draw d, ColorSetting c, float cy) {
        Theme t = Ui.theme();
        int argb = c.argb();
        int[] v = {Colors.red(argb), Colors.green(argb), Colors.blue(argb)};
        int[] tint = {0xFFFF5555, 0xFF55FF55, 0xFF5599FF};
        for (int i = 0; i < 3; i++) {
            float yy = cy + i * CHANNEL;
            d.rrect(x + 14, yy + 4, w - 40, 3, 1, t.panelAlt());
            d.rrect(x + 14, yy + 4, Math.max(2, (w - 40) * v[i] / 255f), 3, 1, tint[i]);
            d.rect(x + 14 + (w - 40) * v[i] / 255f - 1, yy + 2, 3, 7, 0xFFFFFFFF);
            d.text("RGB".substring(i, i + 1), x + 5, yy + 1, tint[i], false);
            d.textRight(Integer.toString(v[i]), x + w - 4, yy + 1, t.textDim(), false);
        }
    }

    // ---- input ----

    private Row rowAt(double mx, double my) {
        for (Row r : rows) if (mx >= x && mx < x + w && my >= r.y && my < r.y + r.h) return r;
        return null;
    }

    private void commitText() {
        if (textEdit != null) {
            textEdit.parse(field.text());
            field.setFocused(false);
            textEdit = null;
        }
    }

    private void cancelCaptures() {
        commitText();
        keyCapture = null;
    }

    /** Returns true if the click hit a row (and was handled). */
    public boolean mouseDown(double mx, double my, int button) {
        Row r = rowAt(mx, my);
        if (r == null) {
            cancelCaptures();
            return false;
        }
        Setting<?> s = r.setting;
        if (s != textEdit) commitText();
        if (s != keyCapture) keyCapture = null;
        double local = my - r.y;

        if (s instanceof BoolSetting b) {
            if (local < baseHeight(s)) b.toggle();
        } else if (s instanceof NumberSetting n) {
            if (local < baseHeight(s)) {
                draggingNumber = n;
                setFromMouse(n, mx);
            }
        } else if (s instanceof ModeSetting m) {
            if (local < ROW) {
                if (button == 1) m.cycle(-1);
                else openDropdown = openDropdown == s ? null : s;
            } else if (s == openDropdown) {
                int idx = (int) ((local - ROW - 1) / OPTION);
                if (idx >= 0 && idx < m.options().size()) m.set(m.options().get(idx));
                openDropdown = null;
            }
        } else if (s instanceof ColorSetting c) {
            if (local < ROW) {
                if (mx >= x + w - 18) openColor = openColor == s ? null : s;
                else beginText(s, c.display());
            } else if (s == openColor) {
                draggingColor = c;
                draggingChannel = Math.max(0, Math.min(2, (int) ((local - ROW - 1) / CHANNEL)));
                setChannel(c, mx);
            }
        } else if (s instanceof TextSetting) {
            if (local < ROW) beginText(s, ((TextSetting) s).get());
        } else if (s instanceof KeybindSetting) {
            keyCapture = s;
        }
        return true;
    }

    private void beginText(Setting<?> s, String initial) {
        textEdit = s;
        field.setText(initial);
        field.setFocused(true);
    }

    private void setFromMouse(NumberSetting n, double mx) {
        n.setFraction((mx - (x + 4)) / (w - 8));
    }

    private void setChannel(ColorSetting c, double mx) {
        int v = (int) Math.round(Math.max(0, Math.min(1, (mx - (x + 14)) / (w - 40))) * 255);
        int argb = c.argb();
        int r = Colors.red(argb), g = Colors.green(argb), b = Colors.blue(argb);
        if (draggingChannel == 0) r = v; else if (draggingChannel == 1) g = v; else b = v;
        c.set(Colors.argb(Colors.alpha(argb), r, g, b));
    }

    public void mouseUp(double mx, double my, int button) {
        draggingNumber = null;
        draggingColor = null;
    }

    public void mouseDrag(double mx, double my, int button) {
        if (draggingNumber != null) setFromMouse(draggingNumber, mx);
        if (draggingColor != null) setChannel(draggingColor, mx);
    }

    /** Returns true if the key was consumed by keybind capture or text editing. */
    public boolean keyDown(int key, int scancode, int modifiers) {
        if (keyCapture instanceof KeybindSetting k) {
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) k.set(-1);
            else if (key != GLFW.GLFW_KEY_ESCAPE) k.set(key);
            keyCapture = null;
            return true;
        }
        if (textEdit != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { commitText(); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { field.setFocused(false); textEdit = null; return true; }
            return field.onKey(key, modifiers) || true;
        }
        return false;
    }

    public void charTyped(int codePoint) {
        if (textEdit != null) field.onChar(codePoint);
    }

    public boolean capturingInput() { return keyCapture != null || textEdit != null; }

    /** Drop transient editing state (call when switching panels). */
    public void reset() {
        openDropdown = null;
        openColor = null;
        keyCapture = null;
        textEdit = null;
        field.setFocused(false);
    }
}
