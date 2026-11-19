package dev.iris.client.modules;

import dev.iris.client.module.Category;
import dev.iris.client.module.Module;
import dev.iris.client.module.ModuleManager;
import dev.iris.client.render.Anim;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ColorSetting;
import dev.iris.client.setting.KeybindSetting;
import dev.iris.client.setting.ModeSetting;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.util.Format;
import dev.iris.client.util.Input;
import dev.iris.client.util.Mc;
import net.minecraft.client.OptionInstance;
import org.lwjgl.glfw.GLFW;

/** Render-category modules: client-side display preferences and screen overlays. Nothing here affects the server. */
public final class RenderModules {
    private RenderModules() {}

    public static void register(ModuleManager mm) {
        mm.register(
                new Fullbright(),
                new FovChanger(),
                new Zoom(),
                new NoViewBob(),
                new CustomCrosshair(),
                new DamageFlash(),
                new LowHealthVignette(),
                new Letterbox(),
                new ScreenTint(),
                new CompassBar());
    }

    // ------------------------------------------------------------------ helpers

    /** Option access. Vanilla validates values (gamma 0..1, fov 30..110); we write the field directly when we can. */
    private static final class Opts {
        private static java.lang.reflect.Field valueField;

        private Opts() {}

        /** Writes the raw value of an option, bypassing range validation. Returns false if reflection failed. */
        static <T> boolean force(OptionInstance<T> option, T value) {
            try {
                if (valueField == null) {
                    java.lang.reflect.Field f = OptionInstance.class.getDeclaredField("value");
                    f.setAccessible(true);
                    valueField = f;
                }
                valueField.set(option, value);
                return true;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return false;
            }
        }
    }

    /** Draws a soft colored border fading toward the screen center. {@code frac} is the border depth (0..0.5). */
    private static void edgeGlow(Draw d, int color, float frac) {
        int w = Mc.guiWidth();
        int h = Mc.guiHeight();
        int clear = Colors.withAlpha(color, 0);
        float th = h * frac;
        float tw = w * frac;
        d.gradientV(0, 0, w, th, color, clear);
        d.gradientV(0, h - th, w, th, clear, color);
        d.gradientH(0, 0, tw, h, color, clear);
        d.gradientH(w - tw, 0, tw, h, clear, color);
    }

    // ------------------------------------------------------------------ modules

    /** Raises the gamma option so dark areas are fully lit. Restores the previous brightness on disable. */
    private static final class Fullbright extends Module {
        private final NumberSetting gamma = add(new NumberSetting("Gamma", "Brightness value to apply (1 is the vanilla maximum).", 12, 1, 16, 1));
        private Double original;

        Fullbright() {
            super("Fullbright", "Lights up dark areas by raising the brightness option.", Category.RENDER);
            gamma.onChange(v -> { if (isEnabled()) apply(v); });
        }

        @Override protected boolean requiresWorld() { return false; }

        private void apply(double value) {
            OptionInstance<Double> opt = mc.options.gamma();
            if (!Opts.force(opt, value)) opt.set(1.0);
        }

        @Override protected void onEnable() {
            if (original == null) original = mc.options.gamma().get();
            apply(gamma.dbl());
        }

        @Override protected void onDisable() {
            if (original != null) {
                OptionInstance<Double> opt = mc.options.gamma();
                if (!Opts.force(opt, original)) opt.set(Math.min(1.0, original));
                original = null;
            }
        }

        @Override public String info() { return gamma.display(); }
    }

    /** Sets the field of view beyond the slider range of the options screen. */
    private static final class FovChanger extends Module {
        private final NumberSetting fov = add(new NumberSetting("Fov", "Field of view in degrees.", 90, 30, 140, 1));
        private Integer original;

        FovChanger() {
            super("FOV Changer", "Sets your field of view to a custom value.", Category.RENDER);
            fov.onChange(v -> { if (isEnabled()) apply(v); });
        }

        @Override protected boolean requiresWorld() { return false; }

        private void apply(double value) {
            int v = (int) Math.round(value);
            if (!Opts.force(mc.options.fov(), v)) mc.options.fov().set(Math.max(30, Math.min(110, v)));
        }

        @Override protected void onEnable() {
            if (original == null) original = mc.options.fov().get();
            apply(fov.dbl());
        }

        @Override protected void onDisable() {
            if (original != null) {
                if (!Opts.force(mc.options.fov(), original)) mc.options.fov().set(original);
                original = null;
            }
        }

        @Override public String info() { return fov.display(); }
    }

    /** Hold a key to smoothly narrow the field of view; releasing it glides back to the previous value. */
    private static final class Zoom extends Module {
        private final KeybindSetting key = add(new KeybindSetting("Zoom Key", "Hold this key to zoom.", GLFW.GLFW_KEY_C));
        private final NumberSetting zoomFov = add(new NumberSetting("Zoom Fov", "Field of view while zooming.", 30, 5, 60, 1));
        private final NumberSetting speed = add(new NumberSetting("Speed", "How quickly the zoom glides in and out.", 10, 3, 24, 1));
        private boolean active;
        private float factor;
        private int original = 70;

        Zoom() {
            super("Zoom", "Hold a key to zoom in with a smooth field of view transition.", Category.RENDER);
        }

        @Override public void onRender(Draw draw, float partialTick) {
            boolean hold = mc.gui.screen() == null && key.isBound() && Input.isKeyDown(key.keyCode());
            if (!active) {
                if (!hold) return;
                original = mc.options.fov().get(); // captured at zoom start so later option changes are respected
                factor = 0f;
                active = true;
            }
            factor = Anim.approach(factor, hold ? 1f : 0f, speed.flt());
            if (!hold && factor <= 0f) {
                Opts.force(mc.options.fov(), original);
                active = false;
                return;
            }
            int value = Math.round(original + (zoomFov.flt() - original) * factor);
            Opts.force(mc.options.fov(), value);
        }

        @Override protected void onDisable() {
            if (active) Opts.force(mc.options.fov(), original);
            active = false;
            factor = 0f;
        }

        @Override public String info() { return key.display(); }
    }

    /** Turns off the walking camera bob. */
    private static final class NoViewBob extends Module {
        private Boolean original;

        NoViewBob() {
            super("No View Bob", "Disables the camera bobbing while walking.", Category.RENDER);
        }

        @Override protected boolean requiresWorld() { return false; }

        @Override protected void onEnable() {
            if (original == null) original = mc.options.bobView().get();
            mc.options.bobView().set(false);
        }

        @Override protected void onDisable() {
            if (original != null) {
                mc.options.bobView().set(original);
                original = null;
            }
        }
    }

    /** Draws a configurable crosshair at the screen center, on top of the vanilla one. */
    private static final class CustomCrosshair extends Module {
        private final NumberSetting size = add(new NumberSetting("Size", "Length of each arm in pixels.", 5, 1, 20, 1));
        private final NumberSetting gap = add(new NumberSetting("Gap", "Empty space around the center in pixels.", 3, 0, 15, 1));
        private final NumberSetting thickness = add(new NumberSetting("Thickness", "Line thickness in pixels.", 1, 1, 6, 1));
        private final ColorSetting color = add(new ColorSetting("Color", "Crosshair color.", 0xFF55FFFF, true));
        private final BoolSetting outline = add(new BoolSetting("Outline", "Draws a dark outline around the lines.", true));
        private final BoolSetting dot = add(new BoolSetting("Center Dot", "Draws a dot in the middle.", false));
        private final BoolSetting tStyle = add(new BoolSetting("T Style", "Omits the top arm.", false));

        CustomCrosshair() {
            super("Custom Crosshair", "Draws a customizable crosshair at the center of the screen.", Category.RENDER);
        }

        @Override public void onRender(Draw d, float partialTick) {
            if (mc.gui.screen() != null) return;
            float cx = Mc.guiWidth() / 2f;
            float cy = Mc.guiHeight() / 2f;
            float s = size.flt();
            float g = gap.flt();
            float t = thickness.flt();
            float half = t / 2f;
            int c = color.argb();
            int dark = Colors.argb(Math.min(255, Colors.alpha(c)), 0, 0, 0);

            if (outline.on()) {
                arms(d, cx, cy, s + 2, g - 1, t + 2, half + 1, dark, true);
                if (dot.on()) d.rect(cx - half - 1, cy - half - 1, t + 2, t + 2, dark);
            }
            arms(d, cx, cy, s, g, t, half, c, false);
            if (dot.on()) d.rect(cx - half, cy - half, t, t, c);
        }

        private void arms(Draw d, float cx, float cy, float s, float g, float t, float half, int c, boolean outer) {
            float inner = Math.max(0, g);
            float len = outer ? s : s;
            d.rect(cx - inner - len, cy - half, len, t, c);          // left
            d.rect(cx + inner, cy - half, len, t, c);                // right
            d.rect(cx - half, cy + inner, t, len, c);                // bottom
            if (!tStyle.on()) d.rect(cx - half, cy - inner - len, t, len, c); // top
        }
    }

    /** Flashes a red screen border when you lose health. */
    private static final class DamageFlash extends Module {
        private final ColorSetting color = add(new ColorSetting("Color", "Flash color.", 0xFFFF2020));
        private final NumberSetting intensity = add(new NumberSetting("Intensity", "Maximum flash opacity in percent.", 70, 10, 100, 5));
        private final NumberSetting fade = add(new NumberSetting("Fade Speed", "How fast the flash fades.", 5, 1, 12, 1));
        private float lastHealth = -1f;
        private float flash;

        DamageFlash() {
            super("Damage Flash", "Flashes the screen edges red when you take damage.", Category.RENDER);
        }

        @Override protected void onEnable() { lastHealth = -1f; flash = 0f; }

        @Override public void onWorldLeave() { lastHealth = -1f; flash = 0f; }

        @Override public void onTick() {
            float hp = mc.player.getHealth();
            if (lastHealth >= 0f && hp < lastHealth - 0.01f) {
                flash = Math.min(1f, 0.45f + (lastHealth - hp) / 8f);
            }
            lastHealth = hp;
        }

        @Override public void onRender(Draw d, float partialTick) {
            if (flash <= 0f) return;
            flash = Anim.approach(flash, 0f, fade.flt());
            if (flash <= 0f) return;
            int c = Colors.withAlpha(color.argb(), Math.round(255 * flash * intensity.flt() / 100f));
            edgeGlow(d, c, 0.28f);
        }
    }

    /** Pulsing red vignette while your health is below a threshold. */
    private static final class LowHealthVignette extends Module {
        private final NumberSetting threshold = add(new NumberSetting("Threshold", "Health points (2 per heart) at or below which the vignette shows.", 6, 2, 20, 1));
        private final ColorSetting color = add(new ColorSetting("Color", "Vignette color.", 0xFFC01010));
        private final BoolSetting pulse = add(new BoolSetting("Pulse", "Pulses like a heartbeat.", true));

        LowHealthVignette() {
            super("Low Health Vignette", "Shows a red screen border while your health is low.", Category.RENDER);
        }

        @Override public void onRender(Draw d, float partialTick) {
            float hp = mc.player.getHealth();
            float limit = threshold.flt();
            if (hp > limit || hp <= 0f) return;
            float severity = 1f - Math.max(0f, hp - 1f) / limit;
            float a = 0.35f + 0.55f * Math.max(0f, Math.min(1f, severity));
            if (pulse.on()) {
                double phase = (System.currentTimeMillis() % 1000L) / 1000.0 * Math.PI * 2;
                a *= 0.7f + 0.3f * (float) Math.sin(phase);
            }
            edgeGlow(d, Colors.withAlpha(color.argb(), Math.round(255 * a)), 0.3f);
        }
    }

    /** Cinematic black bars at the top and bottom of the screen. */
    private static final class Letterbox extends Module {
        private final NumberSetting height = add(new NumberSetting("Bar Height", "Bar height as a percentage of the screen.", 12, 2, 35, 1));
        private final ColorSetting color = add(new ColorSetting("Color", "Bar color.", 0xFF000000));
        private final Anim.Smooth open = new Anim.Smooth(0f, 8f);

        Letterbox() {
            super("Letterbox", "Adds cinematic bars above and below the view.", Category.RENDER);
        }

        @Override protected void onEnable() { open.snap(0f); }

        @Override public void onRender(Draw d, float partialTick) {
            float p = open.update(1f);
            float bar = Mc.guiHeight() * height.flt() / 100f * p;
            int w = Mc.guiWidth();
            d.rect(0, 0, w, bar, color.argb());
            d.rect(0, Mc.guiHeight() - bar, w, bar, color.argb());
        }

        @Override public String info() { return height.display() + "%"; }
    }

    /** Overlays a translucent color over the whole view. */
    private static final class ScreenTint extends Module {
        private final ColorSetting color = add(new ColorSetting("Color", "Tint color; the alpha channel sets its strength.", 0x331E90FF, true));

        ScreenTint() {
            super("Screen Tint", "Applies a translucent color over the screen.", Category.RENDER);
        }

        @Override public void onRender(Draw d, float partialTick) {
            d.rect(0, 0, Mc.guiWidth(), Mc.guiHeight(), color.argb());
        }
    }

    /** A heading strip at the top of the screen with cardinal directions and a degree readout. */
    private static final class CompassBar extends Module {
        private static final String[] POINTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

        private final NumberSetting width = add(new NumberSetting("Width", "Strip width in pixels.", 200, 100, 400, 10));
        private final NumberSetting span = add(new NumberSetting("Field", "Degrees visible across the strip.", 120, 60, 240, 10));
        private final NumberSetting offsetY = add(new NumberSetting("Offset Y", "Distance from the top of the screen.", 6, 0, 60, 1));
        private final ColorSetting accent = add(new ColorSetting("Accent", "Color of the center marker and cardinal letters.", 0xFF55FFFF));
        private final BoolSetting degrees = add(new BoolSetting("Show Degrees", "Shows the numeric heading under the strip.", true));

        CompassBar() {
            super("Compass Bar", "Shows a compass strip with your heading at the top of the screen.", Category.RENDER);
        }

        @Override public void onRender(Draw d, float partialTick) {
            float yaw = mc.player.getYRot();
            float bearing = (((yaw + 180f) % 360f) + 360f) % 360f; // 0 = north, 90 = east
            float w = width.flt();
            float sp = span.flt();
            float x0 = (Mc.guiWidth() - w) / 2f;
            float y0 = offsetY.flt();
            float h = 14f;
            float perDeg = w / sp;
            float cx = x0 + w / 2f;

            d.rect(x0, y0, w, h, 0x80000000);
            d.scissor(x0, y0, w, h + 1);
            int first = (int) Math.floor((bearing - sp / 2f) / 15f) * 15;
            for (int a = first; a <= bearing + sp / 2f; a += 15) {
                float x = cx + (a - bearing) * perDeg;
                int norm = ((a % 360) + 360) % 360;
                if (norm % 45 == 0) {
                    String label = POINTS[norm / 45];
                    d.textCentered(label, x, y0 + 3, norm % 90 == 0 ? accent.argb() : 0xFFFFFFFF, true);
                } else {
                    d.rect(x, y0 + h - 5, 1, 4, 0x99FFFFFF);
                }
            }
            d.endScissor();
            d.rect(cx - 0.5f, y0 + h, 1, 3, accent.argb());
            if (degrees.on()) d.textCentered(Format.fixed(bearing, 0) + "°", cx, y0 + h + 5, 0xFFDDDDDD, true);
        }

        @Override public String info() { return ""; }
    }
}
