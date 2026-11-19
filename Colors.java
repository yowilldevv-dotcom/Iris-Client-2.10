package dev.iris.client.render;

/** ARGB color math. */
public final class Colors {
    private Colors() {}

    public static int alpha(int argb) { return argb >>> 24; }
    public static int red(int argb) { return (argb >> 16) & 0xFF; }
    public static int green(int argb) { return (argb >> 8) & 0xFF; }
    public static int blue(int argb) { return argb & 0xFF; }

    public static int rgb(int r, int g, int b) { return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b); }

    public static int argb(int a, int r, int g, int b) { return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b); }

    public static int withAlpha(int argb, int alpha) { return (clamp(alpha) << 24) | (argb & 0xFFFFFF); }

    /** Multiplies the existing alpha by {@code factor} (0..1). */
    public static int mulAlpha(int argb, float factor) {
        return withAlpha(argb, Math.round(alpha(argb) * Math.max(0f, Math.min(1f, factor))));
    }

    public static int lerp(int a, int b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return argb(
                Math.round(alpha(a) + (alpha(b) - alpha(a)) * k),
                Math.round(red(a) + (red(b) - red(a)) * k),
                Math.round(green(a) + (green(b) - green(a)) * k),
                Math.round(blue(a) + (blue(b) - blue(a)) * k));
    }

    public static int brighten(int argb, float amount) {
        return lerp(argb, 0xFFFFFFFF, amount);
    }

    public static int darken(int argb, float amount) {
        return lerp(argb, (argb & 0xFF000000) | 0x000000, amount);
    }

    /** Rainbow color cycling over time; {@code offset} staggers the hue. */
    public static int rainbow(float offset, float speed) {
        float hue = ((System.currentTimeMillis() % 100_000L) / 1000f * speed + offset) % 1f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue < 0 ? hue + 1 : hue, 0.7f, 1f) & 0xFFFFFF);
    }

    /** Green→yellow→red scale: 1.0 is healthy (green), 0.0 is critical (red). */
    public static int health(float fraction) {
        float f = Math.max(0f, Math.min(1f, fraction));
        return f > 0.5f ? lerp(0xFFFFE055, 0xFF55FF55, (f - 0.5f) * 2f) : lerp(0xFFFF5555, 0xFFFFE055, f * 2f);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
