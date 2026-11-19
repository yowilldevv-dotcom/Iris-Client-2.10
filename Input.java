package dev.iris.client.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Raw keyboard/mouse polling through GLFW. All window-handle access is isolated here so a Minecraft API change
 * only needs fixing in one place.
 */
public final class Input {
    private Input() {}

    private static long window() {
        return Minecraft.getInstance().getWindow().handle();
    }

    public static boolean isKeyDown(int glfwKey) {
        if (glfwKey < 0) return false;
        return GLFW.glfwGetKey(window(), glfwKey) == GLFW.GLFW_PRESS;
    }

    public static boolean isMouseDown(int glfwButton) {
        return GLFW.glfwGetMouseButton(window(), glfwButton) == GLFW.GLFW_PRESS;
    }

    /** Clipboard text, or "" when unavailable. */
    public static String clipboard() {
        try {
            String s = GLFW.glfwGetClipboardString(window());
            return s == null ? "" : s;
        } catch (RuntimeException e) {
            return "";
        }
    }

    public static void setClipboard(String text) {
        try {
            GLFW.glfwSetClipboardString(window(), text);
        } catch (RuntimeException ignored) {
            // clipboard unavailable
        }
    }

    public static boolean ctrlDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean shiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
