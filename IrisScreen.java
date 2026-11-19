package dev.iris.client.gui;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Anim;
import dev.iris.client.render.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Base class for all Iris screens. It is the ONLY place that touches Minecraft's screen input/render signatures:
 * subclasses work with plain doubles/ints in "virtual" coordinates (GUI pixels divided by the Iris GUI scale), so
 * a Minecraft API change is a one-file fix.
 *
 * <p>Draw in {@link #draw}; handle input in the {@code on*} hooks. Use {@link #vw()} / {@link #vh()} for the
 * usable size and never hard-code pixel sizes larger than that, so layouts fit every resolution.
 */
public abstract class IrisScreen extends Screen {
    protected final Screen parent;
    private float scale = 1f;

    protected IrisScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    /** True when the normal HUD must not be drawn behind this screen (e.g. the HUD editor draws its own). */
    public boolean suppressesHud() { return false; }

    /** Whether to dim the background behind the screen. */
    protected boolean dimBackground() { return true; }

    /** Virtual width/height after applying the Iris GUI scale. */
    protected final float vw() { return width / scale; }
    protected final float vh() { return height / scale; }
    protected final float uiScale() { return scale; }

    // ---- hooks for subclasses ----

    protected abstract void draw(Draw d, float mouseX, float mouseY, float delta);

    protected void onMouseDown(double x, double y, int button) {}
    protected void onMouseUp(double x, double y, int button) {}
    protected void onMouseDrag(double x, double y, int button, double dx, double dy) {}
    protected void onScroll(double x, double y, double amount) {}

    /** Return true if the key was consumed (otherwise Escape closes the screen). */
    protected boolean onKeyDown(int key, int scancode, int modifiers) { return false; }

    protected void onChar(int codePoint) {}

    protected void onOpen() {}

    protected void onClosed() {}

    // ---- Minecraft glue ----

    @Override
    protected void init() {
        super.init();
        onOpen();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Anim.frame();
        scale = Iris.client().guiScale.flt();
        Draw d = Draw.of(graphics);
        if (dimBackground()) d.rect(0, 0, width, height, Iris.themes().current().background());
        d.pushScaled(0, 0, scale);
        try {
            draw(d, mouseX / scale, mouseY / scale, delta);
        } finally {
            d.pop();
        }
        Iris.notifications().render(Draw.of(graphics), width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        onMouseDown(event.x() / scale, event.y() / scale, event.button());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        onMouseUp(event.x() / scale, event.y() / scale, event.button());
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        onMouseDrag(event.x() / scale, event.y() / scale, event.button(), dx / scale, dy / scale);
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        onScroll(x / scale, y / scale, scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (onKeyDown(event.key(), event.scancode(), event.modifiers())) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        onChar(event.codepoint());
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        try {
            onClosed();
        } finally {
            Minecraft.getInstance().gui.setScreen(parent);
        }
    }
}
