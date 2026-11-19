package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.util.Input;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.List;

/** Clicks per second for both mouse buttons (rolling one second window of button presses). */
public final class CpsElement extends TextHudElement {
    private final ArrayDeque<Long> left = new ArrayDeque<>();
    private final ArrayDeque<Long> right = new ArrayDeque<>();
    private boolean leftDown;
    private boolean rightDown;

    public CpsElement() {
        super("CPS", "Shows clicks per second.", 0.01, 0.13, false);
    }

    @Override public void prepare(boolean preview) {
        long now = System.currentTimeMillis();
        boolean l = Input.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        boolean r = Input.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        if (l && !leftDown) left.add(now);
        if (r && !rightDown) right.add(now);
        leftDown = l;
        rightDown = r;
        while (!left.isEmpty() && now - left.peekFirst() > 1000) left.pollFirst();
        while (!right.isEmpty() && now - right.peekFirst() > 1000) right.pollFirst();
        super.prepare(preview);
    }

    @Override protected List<String> lines(boolean preview) {
        if (preview) return List.of("CPS: 8 | 3");
        return List.of("CPS: " + left.size() + " | " + right.size());
    }
}
