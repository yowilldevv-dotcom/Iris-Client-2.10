package dev.iris.client.render;

/**
 * Frame-time based animation helpers. Call {@link #frame()} once per rendered frame (the hooks in IrisClient do
 * this); everything else reads the cached delta so animations are frame-rate independent.
 */
public final class Anim {
    private static long lastNanos = System.nanoTime();
    private static float delta = 0.016f;
    private static boolean enabled = true;
    private static float speed = 1f;

    private Anim() {}

    /** Updates the per-frame delta. Safe to call multiple times per frame (the first call after a gap wins). */
    public static void frame() {
        long now = System.nanoTime();
        float d = (now - lastNanos) / 1_000_000_000f;
        if (d > 0.0005f) {
            delta = Math.min(d, 0.1f);
            lastNanos = now;
        }
    }

    public static void configure(boolean animationsEnabled, float animationSpeed) {
        enabled = animationsEnabled;
        speed = Math.max(0.25f, animationSpeed);
    }

    public static float delta() { return delta; }
    public static boolean enabled() { return enabled; }

    /** Moves {@code current} toward {@code target} exponentially. {@code rate} ~ 8 is snappy, ~ 14 is very fast. */
    public static float approach(float current, float target, float rate) {
        if (!enabled) return target;
        float k = 1f - (float) Math.exp(-rate * speed * delta);
        float next = current + (target - current) * k;
        return Math.abs(target - next) < 0.001f ? target : next;
    }

    public static float easeOutCubic(float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return 1f - (float) Math.pow(1f - k, 3);
    }

    /** A value that glides toward a target. */
    public static final class Smooth {
        private float value;
        private final float rate;

        public Smooth(float initial, float rate) {
            this.value = initial;
            this.rate = rate;
        }

        public float update(float target) {
            value = approach(value, target, rate);
            return value;
        }

        public float get() { return value; }

        public void snap(float v) { value = v; }
    }
}
