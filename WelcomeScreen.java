package dev.iris.client.gui;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.theme.Theme;
import dev.iris.client.util.Mc;
import net.minecraft.client.gui.screens.Screen;

/** Startup screen: branding, versions, a drifting star field and quick links to the main Iris screens. */
public final class WelcomeScreen extends IrisScreen {
    private static final String[] LABELS = {"ClickGUI", "HUD Editor", "Configurations", "Themes", "Settings", "Account"};
    private static final int STARS = 70;

    private final long opened = System.currentTimeMillis();
    private final float[][] buttonRects = new float[LABELS.length + 1][4];

    public WelcomeScreen(Screen parent) { super("Welcome", parent); }

    @Override protected void onClosed() { Ui.clearAnims(); }

    private static float hash(int n, int salt) {
        int h = n * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    @Override
    protected void draw(Draw d, float mx, float my, float delta) {
        Theme t = Ui.theme();
        float time = (System.currentTimeMillis() - opened) / 1000f;
        float w = vw();
        float h = vh();

        for (int i = 0; i < STARS; i++) {
            float sx = (hash(i, 1) * w + time * (2f + hash(i, 5) * 6f)) % w;
            float sy = hash(i, 2) * h;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(time * (0.8f + hash(i, 3) * 2f) + i));
            float size = hash(i, 4) > 0.9f ? 2f : 1f;
            d.rect(sx, sy, size, size, Colors.mulAlpha(Colors.lerp(t.accent2(), 0xFFFFFFFF, 0.5f), tw * 0.8f));
        }

        float fade = Math.min(1f, time / 0.6f);
        boolean compact = h < 250f;
        float btnW = Math.min(130f, (w - 24f) / 2f);
        float btnH = 18f;
        float gap = 4f;
        int rows = 4; // 3 rows of two + continue
        float logoBlock = compact ? 40f : 78f;
        float total = logoBlock + rows * (btnH + gap);
        float top = Math.max(6f, (h - total) / 2f);

        float cx = w / 2f;
        float lift = (1f - Anim2.ease(fade)) * 10f;
        if (!compact) {
            Screens.drawLogoMark(d, cx, top + 16 - lift, 14);
            float scale = Math.min(3.5f, (w - 20f) / Math.max(1, d.width("Iris")) / 1.4f);
            float tw = d.width("Iris") * scale;
            Screens.gradientText(d, "Iris", cx - tw / 2f, top + 34 - lift, scale);
            d.textCentered("Client " + Iris.version() + "  |  Minecraft " + Iris.minecraftVersion(), cx, top + 34 + 9 * scale + 4 - lift, Colors.mulAlpha(t.textDim(), fade), false);
        } else {
            Screens.drawLogoMark(d, cx - 40, top + 10, 8);
            Screens.gradientText(d, "Iris", cx - 24, top + 4, 2f);
            d.textCentered(Iris.version() + " | MC " + Iris.minecraftVersion(), cx, top + 26, t.textDim(), false);
        }

        float by = top + logoBlock;
        float left = cx - btnW - gap / 2f;
        float right = cx + gap / 2f;
        for (int i = 0; i < LABELS.length; i++) {
            float bx = i % 2 == 0 ? left : right;
            float yy = by + (i / 2) * (btnH + gap);
            set(i, bx, yy, btnW, btnH);
            Ui.button(d, LABELS[i], bx, yy, btnW, btnH, mx, my, false);
        }
        float cy = by + 3 * (btnH + gap);
        set(LABELS.length, cx - btnW - gap / 2f, cy, btnW * 2 + gap, btnH);
        Ui.button(d, "Continue", cx - btnW - gap / 2f, cy, btnW * 2 + gap, btnH, mx, my, true);
        d.textCentered("Iris never stores your Minecraft password or session token.", cx, h - 12, Colors.mulAlpha(t.textDim(), 0.7f), false);
    }

    private void set(int i, float x, float y, float w, float h) {
        buttonRects[i][0] = x;
        buttonRects[i][1] = y;
        buttonRects[i][2] = w;
        buttonRects[i][3] = h;
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (button != 0) return;
        for (int i = 0; i < buttonRects.length; i++) {
            float[] r = buttonRects[i];
            if (!Ui.hover(mx, my, r[0], r[1], r[2], r[3])) continue;
            switch (i) {
                case 0 -> Mc.openScreen(Screens.clickGui());
                case 1 -> Mc.openScreen(Screens.hudEditor());
                case 2 -> Mc.openScreen(Screens.configs());
                case 3 -> Mc.openScreen(Screens.themes());
                case 4 -> Mc.openScreen(Screens.settings());
                case 5 -> Mc.openScreen(Screens.account());
                default -> onClose();
            }
            return;
        }
    }

    /** Tiny easing helper kept local to this screen. */
    private static final class Anim2 {
        static float ease(float t) { return dev.iris.client.render.Anim.easeOutCubic(t); }
    }
}
