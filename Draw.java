package dev.iris.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Thin drawing facade over Minecraft's GUI graphics. Every Iris feature draws through this class, so a change in
 * Minecraft's rendering API only needs fixing here. Colors are ARGB ints; coordinates are GUI-scaled pixels.
 */
public final class Draw {
    private static final Draw INSTANCE = new Draw();

    private GuiGraphicsExtractor g;

    private Draw() {}

    /** Returns the shared Draw bound to the given graphics object for this frame. */
    public static Draw of(GuiGraphicsExtractor graphics) {
        INSTANCE.g = graphics;
        return INSTANCE;
    }

    private Font font() { return Minecraft.getInstance().font; }

    // ---- shapes ----

    public void rect(float x, float y, float w, float h, int argb) {
        if (w <= 0 || h <= 0 || (argb >>> 24) == 0) return;
        g.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), argb);
    }

    /** Rectangle with chopped corners (cheap rounded look). Radius is clamped to 1..4. */
    public void rrect(float x, float y, float w, float h, float radius, int argb) {
        if (w <= 0 || h <= 0 || (argb >>> 24) == 0) return;
        int r = (int) Math.max(1, Math.min(4, Math.min(radius, Math.min(w, h) / 2)));
        rect(x, y + r, w, h - 2 * r, argb);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            float inset = (float) (r - Math.sqrt((double) r * r - dy * dy));
            rect(x + inset, y + i, w - 2 * inset, 1, argb);
            rect(x + inset, y + h - i - 1, w - 2 * inset, 1, argb);
        }
    }

    public void outline(float x, float y, float w, float h, int argb) {
        rect(x, y, w, 1, argb);
        rect(x, y + h - 1, w, 1, argb);
        rect(x, y, 1, h, argb);
        rect(x + w - 1, y, 1, h, argb);
    }

    public void gradientV(float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        g.fillGradient(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), top, bottom);
    }

    /** Horizontal gradient built from thin vertical strips (steps capped for performance). */
    public void gradientH(float x, float y, float w, float h, int left, int right) {
        int steps = (int) Math.max(1, Math.min(48, w / 2));
        float sw = w / steps;
        for (int i = 0; i < steps; i++) {
            rect(x + i * sw, y, sw + 1, h, Colors.lerp(left, right, steps == 1 ? 0 : i / (float) (steps - 1)));
        }
    }

    // ---- text ----

    public void text(String s, float x, float y, int argb, boolean shadow) {
        if (s == null || s.isEmpty() || (argb >>> 24) == 0) return;
        g.text(font(), s, Math.round(x), Math.round(y), argb, shadow);
    }

    public void text(String s, float x, float y, int argb) { text(s, x, y, argb, true); }

    public void textCentered(String s, float cx, float y, int argb, boolean shadow) {
        text(s, cx - width(s) / 2f, y, argb, shadow);
    }

    public void textRight(String s, float right, float y, int argb, boolean shadow) {
        text(s, right - width(s), y, argb, shadow);
    }

    public int width(String s) { return s == null ? 0 : font().width(s); }

    public int lineHeight() { return font().lineHeight; }

    /** Draws text clipped with an ellipsis so it fits {@code maxWidth}. */
    public void textFit(String s, float x, float y, float maxWidth, int argb, boolean shadow) {
        if (width(s) <= maxWidth) { text(s, x, y, argb, shadow); return; }
        String t = s;
        while (t.length() > 1 && width(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        text(t + "…", x, y, argb, shadow);
    }

    // ---- clipping / transforms ----

    public void scissor(float x, float y, float w, float h) {
        g.enableScissor(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
    }

    public void endScissor() { g.disableScissor(); }

    /** Pushes a transform that scales around (x, y). Must be paired with {@link #pop()}. */
    public void pushScaled(float x, float y, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
    }

    public void pop() { g.pose().popMatrix(); }

    // ---- items ----

    public void item(ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        g.item(stack, x, y);
    }
}
