package dev.iris.client.hud.elements;

import dev.iris.client.core.Iris;
import dev.iris.client.hud.HudElement;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.ui.KeyFade;
import net.minecraft.client.Minecraft;

/** WASD, jump and mouse buttons with a pressed highlight. */
public final class KeystrokesElement extends HudElement {
    private static final int K = 18;
    private final BoolSetting mouse = add(new BoolSetting("Mouse buttons", "Show LMB and RMB.", true));
    private final KeyFade w = new KeyFade(), a = new KeyFade(), s = new KeyFade(), dd = new KeyFade(),
            sp = new KeyFade(), lm = new KeyFade(), rm = new KeyFade();
    private boolean fw, fa, fs, fd, fj, fl, fr;

    public KeystrokesElement() {
        super("Keystrokes", "Shows which movement and mouse keys are held.", 0.01, 0.7, false);
    }

    @Override public void prepare(boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        fw = mc.options.keyUp.isDown();
        fa = mc.options.keyLeft.isDown();
        fs = mc.options.keyDown.isDown();
        fd = mc.options.keyRight.isDown();
        fj = mc.options.keyJump.isDown();
        fl = mc.options.keyAttack.isDown();
        fr = mc.options.keyUse.isDown();
    }

    @Override public int width(Draw d) { return K * 3 + 4; }

    @Override public int height(Draw d) { return K * 2 + 4 + 8 + (mouse.on() ? K + 2 : 0); }

    private void key(Draw d, KeyFade f, boolean down, String label, int x, int y, int wd, int ht) {
        float p = f.update(down);
        int base = Colors.mulAlpha(Iris.themes().current().panel(), 0.8f);
        d.rrect(x, y, wd, ht, 3, Colors.lerp(base, Iris.themes().current().accent(), p));
        d.textCentered(label, x + wd / 2f, y + (ht - d.lineHeight()) / 2f + 1, color.argb(), shadow.on());
    }

    @Override public void render(Draw d, boolean preview) {
        key(d, w, fw, "W", K + 2, 0, K, K);
        key(d, a, fa, "A", 0, K + 2, K, K);
        key(d, s, fs, "S", K + 2, K + 2, K, K);
        key(d, dd, fd, "D", (K + 2) * 2, K + 2, K, K);
        key(d, sp, fj, "___", 0, (K + 2) * 2, K * 3 + 4, 8);
        if (mouse.on()) {
            int y = (K + 2) * 2 + 10;
            key(d, lm, fl, "LMB", 0, y, K * 3 / 2 + 1, K);
            key(d, rm, fr, "RMB", K * 3 / 2 + 3, y, K * 3 / 2 + 1, K);
        }
    }
}
