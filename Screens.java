package dev.iris.client.gui;

import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Static factory for every Iris screen, plus a few shared branding helpers (logo mark, animated accent bar,
 * gradient title text) drawn purely with rectangles so no textures or emoji glyphs are required.
 */
public final class Screens {
    private Screens() {}

    private static Screen current() { return Minecraft.getInstance().gui.screen(); }

    public static Screen welcome() { return new WelcomeScreen(current()); }
    public static Screen clickGui() { return new ClickGuiScreen(current()); }
    public static Screen hudEditor() { return new HudEditorScreen(current()); }
    public static Screen configs() { return new ConfigScreen(current()); }
    public static Screen themes() { return new ThemeScreen(current()); }
    public static Screen account() { return new AccountScreen(current()); }
    public static Screen settings() { return new SettingsScreen(current()); }

    private static float seconds() { return (System.currentTimeMillis() % 1_000_000L) / 1000f; }

    /** Small rotating spiral galaxy (stands in for the galaxy emoji of the Iris name). */
    public static void drawLogoMark(Draw d, float cx, float cy, float r) {
        Theme t = Ui.theme();
        float rot = seconds() * 0.5f;
        int arms = 2;
        int points = r < 8 ? 10 : 24;
        for (int arm = 0; arm < arms; arm++) {
            for (int i = 1; i <= points; i++) {
                float f = i / (float) points;
                double ang = rot + f * 3.4 + arm * Math.PI;
                float px = cx + (float) Math.cos(ang) * f * r;
                float py = cy + (float) Math.sin(ang) * f * r * 0.6f;
                int c = Colors.mulAlpha(Colors.lerp(t.accent(), t.accent2(), f), 1f - f * 0.55f);
                float size = f < 0.5f && r >= 8 ? 2f : 1f;
                d.rect(px - size / 2f, py - size / 2f, size, size, c);
            }
        }
        d.rect(cx - 1.5f, cy - 1.5f, 3, 3, 0xFFFFFFFF);
        d.rect(cx - 2.5f, cy - 0.5f, 5, 1, Colors.mulAlpha(0xFFFFFFFF, 0.4f));
        d.rect(cx - 0.5f, cy - 2.5f, 1, 5, Colors.mulAlpha(0xFFFFFFFF, 0.4f));
    }

    /** Horizontal accent bar whose gradient slowly slides between accent and accent2. */
    public static void accentBar(Draw d, float x, float y, float w, float h) {
        Theme t = Ui.theme();
        int steps = (int) Math.max(1, Math.min(48, w / 3));
        float sw = w / steps;
        float shift = seconds() * 0.15f;
        for (int i = 0; i < steps; i++) {
            float p = (i / (float) steps + shift) % 1f;
            float tri = p < 0.5f ? p * 2f : (1f - p) * 2f;
            d.rect(x + i * sw, y, sw + 1, h, Colors.lerp(t.accent(), t.accent2(), tri));
        }
    }

    /** Text with a travelling accent gradient, drawn at the given scale with its top-left at (x, y). */
    public static void gradientText(Draw d, String s, float x, float y, float scale) {
        Theme t = Ui.theme();
        float time = seconds();
        d.pushScaled(x, y, scale);
        try {
            float cx = 0;
            for (int i = 0; i < s.length(); i++) {
                String ch = s.substring(i, i + 1);
                float p = (float) (0.5 + 0.5 * Math.sin(time * 1.6 - i * 0.8));
                d.text(ch, cx, 0, Colors.lerp(t.accent(), t.accent2(), p), true);
                cx += d.width(ch);
            }
        } finally {
            d.pop();
        }
    }
}
