package dev.iris.client.gui;

import dev.iris.client.render.Anim;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.theme.Theme;
import dev.iris.client.core.Iris;

import java.util.HashMap;
import java.util.Map;

/** Small immediate-mode widget helpers shared by all Iris screens. All coordinates are virtual GUI pixels. */
public final class Ui {
    private static final Map<String, Anim.Smooth> ANIMS = new HashMap<>();

    private Ui() {}

    public static Theme theme() { return Iris.themes().current(); }

    public static boolean hover(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Smoothly animated 0..1 value keyed by id (hover highlights, toggles, expanders). */
    public static float anim(String key, boolean active) { return anim(key, active ? 1f : 0f, 14f); }

    public static float anim(String key, float target, float rate) {
        Anim.Smooth s = ANIMS.computeIfAbsent(key, k -> new Anim.Smooth(target, rate));
        return s.update(target);
    }

    /** Drops animation state (call when a screen closes to avoid unbounded growth). */
    public static void clearAnims() { ANIMS.clear(); }

    /** Standard button. Returns whether the mouse is over it, so callers can reuse the hit test for clicks. */
    public static boolean button(Draw d, String label, float x, float y, float w, float h, double mx, double my, boolean primary) {
        Theme t = theme();
        boolean over = hover(mx, my, x, y, w, h);
        float a = anim("btn:" + label + ":" + (int) x + ":" + (int) y, over);
        int base = primary ? t.accent() : t.panelAlt();
        int fill = Colors.lerp(base, Colors.brighten(base, 0.18f), a);
        d.rrect(x, y, w, h, 3, fill);
        d.textCentered(label, x + w / 2f, y + (h - d.lineHeight()) / 2f + 1, primary ? contrast(t.accent()) : t.text(), false);
        return over;
    }

    /** Pill-shaped on/off switch, 22x10. */
    public static void toggle(Draw d, float x, float y, boolean on, String animKey) {
        Theme t = theme();
        float a = anim("tg:" + animKey, on);
        d.rrect(x, y, 22, 10, 4, Colors.lerp(t.panelAlt(), t.accent(), a));
        d.rrect(x + 2 + 12 * a, y + 2, 6, 6, 3, Colors.lerp(t.textDim(), 0xFFFFFFFF, a));
    }

    /** Horizontal slider track with filled portion and knob. */
    public static void slider(Draw d, float x, float y, float w, float h, double fraction) {
        Theme t = theme();
        float f = (float) Math.max(0, Math.min(1, fraction));
        d.rrect(x, y + h / 2f - 1, w, 3, 1, t.panelAlt());
        d.rrect(x, y + h / 2f - 1, Math.max(2, w * f), 3, 1, t.accent());
        d.rrect(x + w * f - 3, y + h / 2f - 4, 6, 8, 2, 0xFFFFFFFF);
    }

    /** Black or white, whichever reads better on the given color. */
    public static int contrast(int argb) {
        double lum = 0.299 * Colors.red(argb) + 0.587 * Colors.green(argb) + 0.114 * Colors.blue(argb);
        return lum > 150 ? 0xFF101018 : 0xFFFFFFFF;
    }

    /** Draws the title bar gradient used by windows. */
    public static void header(Draw d, float x, float y, float w, float h) {
        Theme t = theme();
        d.rrect(x, y, w, h, 4, t.header());
        d.gradientH(x + 2, y + h - 2, w - 4, 2, t.accent(), t.accent2());
    }
}
