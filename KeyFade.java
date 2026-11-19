package dev.iris.client.ui;

import dev.iris.client.render.Anim;

/** Animated 0..1 pressed-state value for keystroke style widgets. */
public final class KeyFade {
    private final Anim.Smooth smooth = new Anim.Smooth(0f, 20f);

    public float update(boolean down) { return smooth.update(down ? 1f : 0f); }
}
