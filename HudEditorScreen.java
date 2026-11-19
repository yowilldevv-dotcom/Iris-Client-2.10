package dev.iris.client.gui;

import dev.iris.client.core.Iris;
import dev.iris.client.hud.HudElement;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.theme.Theme;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * On-screen HUD editor. Elements show sample data and can be dragged (with edge/centre snapping), resized with the
 * corner handle or mouse wheel, toggled from the side list, and customised through their settings panel.
 */
public final class HudEditorScreen extends IrisScreen {
    private static final float LIST_W = 112f;
    private static final float PANEL_W = 134f;
    private static final float ROW = 13f;
    private static final float SNAP = 4f;

    private final Map<HudElement, SettingsPanel> panels = new HashMap<>();
    private HudElement selected;
    private HudElement dragging;
    private HudElement resizing;
    private float dragDx, dragDy;
    private float listScroll;
    private float panelScroll;
    private boolean guideX, guideY;
    private float guideXPos, guideYPos;

    public HudEditorScreen(Screen parent) { super("HUD Editor", parent); }

    @Override public boolean suppressesHud() { return true; }

    @Override protected boolean dimBackground() { return false; }

    @Override protected void onClosed() {
        for (SettingsPanel p : panels.values()) p.reset();
        Ui.clearAnims();
    }

    private SettingsPanel panel(HudElement e) { return panels.computeIfAbsent(e, SettingsPanel::new); }

    private float listTop() { return 24f; }
    private float listBottom() { return vh() - 24f; }
    private boolean panelVisible() { return selected != null && vw() > LIST_W + PANEL_W + 140f; }
    private float panelX() { return vw() - PANEL_W - 4f; }

    @Override
    protected void draw(Draw d, float mx, float my, float delta) {
        Theme t = Ui.theme();
        d.rect(0, 0, vw(), vh(), Colors.mulAlpha(t.background(), 0.55f));
        Iris.hud().render(d, (int) vw(), (int) vh(), true);

        // Outlines around enabled elements, brighter for the selection.
        for (HudElement e : Iris.hud().all()) {
            if (!e.isEnabled()) continue;
            boolean sel = e == selected;
            int c = sel ? t.accent() : Colors.mulAlpha(t.textDim(), 0.55f);
            d.outline(e.boundsX() - 1, e.boundsY() - 1, e.boundsW() + 2, e.boundsH() + 2, c);
            if (sel) d.rect(e.boundsX() + e.boundsW() - 3, e.boundsY() + e.boundsH() - 3, 6, 6, t.accent());
        }
        if (dragging != null) {
            if (guideX) d.rect(guideXPos, 0, 1, vh(), Colors.mulAlpha(t.accent2(), 0.8f));
            if (guideY) d.rect(0, guideYPos, vw(), 1, Colors.mulAlpha(t.accent2(), 0.8f));
        }

        drawTopBar(d, mx, my);
        drawList(d, mx, my);
        if (panelVisible()) drawPanel(d, mx, my);
        d.textCentered("Drag to move  |  corner or wheel to resize  |  arrows nudge  |  Del hides", vw() / 2f, vh() - 12, Colors.mulAlpha(t.text(), 0.75f), true);
    }

    private void drawTopBar(Draw d, float mx, float my) {
        Theme t = Ui.theme();
        d.rect(0, 0, vw(), 20, Colors.withAlpha(t.header(), 240));
        d.text("HUD Editor", 8, 6, t.text(), false);
        Ui.button(d, "Reset all", vw() - 100, 3, 48, 14, mx, my, false);
        Ui.button(d, "Done", vw() - 48, 3, 42, 14, mx, my, true);
    }

    private void drawList(Draw d, float mx, float my) {
        Theme t = Ui.theme();
        float top = listTop();
        float h = listBottom() - top;
        d.rrect(4, top, LIST_W, h, 4, Colors.mulAlpha(t.panel(), 0.93f));
        List<HudElement> all = Iris.hud().all();
        float max = Math.max(0, all.size() * ROW - h + 4);
        listScroll = Math.max(0, Math.min(max, listScroll));
        d.scissor(4, top, LIST_W, h);
        float y = top + 2 - listScroll;
        for (HudElement e : all) {
            boolean over = Ui.hover(mx, my, 4, y, LIST_W, ROW) && my >= top && my < top + h;
            if (e == selected) d.rect(5, y, LIST_W - 2, ROW, Colors.mulAlpha(t.accent(), 0.25f));
            else if (over) d.rect(5, y, LIST_W - 2, ROW, Colors.mulAlpha(t.text(), 0.07f));
            d.rrect(9, y + 3, 7, 7, 2, e.isEnabled() ? t.accent() : t.panelAlt());
            d.textFit(e.name(), 21, y + 3, LIST_W - 24, e.isEnabled() ? t.text() : t.textDim(), false);
            y += ROW;
        }
        d.endScissor();
    }

    private void drawPanel(Draw d, float mx, float my) {
        Theme t = Ui.theme();
        float x = panelX();
        float top = listTop();
        float h = listBottom() - top;
        d.rrect(x, top, PANEL_W, h, 4, Colors.mulAlpha(t.panel(), 0.95f));
        d.textFit(selected.name(), x + 6, top + 4, PANEL_W - 12, t.accent(), false);
        float bY = top + h - 18;
        Ui.button(d, "Reset element", x + 4, bY, PANEL_W - 8, 14, mx, my, false);

        float ly = top + 17;
        float lh = bY - ly - 2;
        SettingsPanel p = panel(selected);
        float content = p.layout(x + 2, ly - panelScroll, PANEL_W - 4);
        float max = Math.max(0, content - lh);
        panelScroll = Math.max(0, Math.min(max, panelScroll));
        d.scissor(x, ly, PANEL_W, lh);
        p.layout(x + 2, ly - panelScroll, PANEL_W - 4);
        p.draw(d, mx, my);
        d.endScissor();
    }

    // ---- input ----

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        float top = listTop();
        if (my < 20) {
            if (Ui.hover(mx, my, vw() - 48, 3, 42, 14)) onClose();
            else if (Ui.hover(mx, my, vw() - 100, 3, 48, 14)) { Iris.hud().resetAll(); selected = null; }
            return;
        }
        if (Ui.hover(mx, my, 4, top, LIST_W, listBottom() - top)) {
            int idx = (int) ((my - top - 2 + listScroll) / ROW);
            List<HudElement> all = Iris.hud().all();
            if (idx >= 0 && idx < all.size()) {
                HudElement e = all.get(idx);
                if (mx < 18 || button == 1) e.setEnabled(!e.isEnabled());
                selected = e;
                panelScroll = 0;
            }
            return;
        }
        if (panelVisible() && Ui.hover(mx, my, panelX(), top, PANEL_W, listBottom() - top)) {
            float bY = top + (listBottom() - top) - 18;
            if (Ui.hover(mx, my, panelX() + 4, bY, PANEL_W - 8, 14)) selected.resetAll();
            else panel(selected).mouseDown(mx, my, button);
            return;
        }
        if (selected != null && selected.isEnabled()
                && Ui.hover(mx, my, selected.boundsX() + selected.boundsW() - 5, selected.boundsY() + selected.boundsH() - 5, 10, 10)) {
            resizing = selected;
            return;
        }
        HudElement hit = Iris.hud().elementAt(mx, my);
        if (hit != null) {
            selected = hit;
            dragging = hit;
            dragDx = (float) mx - hit.boundsX();
            dragDy = (float) my - hit.boundsY();
        } else {
            selected = null;
        }
    }

    @Override
    protected void onMouseDrag(double mx, double my, int button, double dx, double dy) {
        if (resizing != null) {
            float base = resizing.boundsW() / resizing.scale();
            if (base > 1) resizing.setScale((float) ((mx - resizing.boundsX()) / base));
            return;
        }
        if (dragging != null) {
            HudElement e = dragging;
            float nx = (float) mx - dragDx;
            float ny = (float) my - dragDy;
            float w = e.boundsW();
            float h = e.boundsH();
            nx = Math.max(0, Math.min(vw() - w, nx));
            ny = Math.max(0, Math.min(vh() - h, ny));
            guideX = guideY = false;
            float[] xs = {0, (vw() - w) / 2f, vw() - w};
            float[] ys = {0, (vh() - h) / 2f, vh() - h};
            for (float s : xs) if (Math.abs(nx - s) < SNAP) { nx = s; guideX = true; guideXPos = s == xs[1] ? vw() / 2f : (s == 0 ? 0 : vw() - 1); break; }
            for (float s : ys) if (Math.abs(ny - s) < SNAP) { ny = s; guideY = true; guideYPos = s == ys[1] ? vh() / 2f : (s == 0 ? 0 : vh() - 1); break; }
            e.setPosition(nx / vw(), ny / vh());
            return;
        }
        if (selected != null) panel(selected).mouseDrag(mx, my, button);
    }

    @Override
    protected void onMouseUp(double mx, double my, int button) {
        dragging = null;
        resizing = null;
        guideX = guideY = false;
        for (SettingsPanel p : panels.values()) p.mouseUp(mx, my, button);
    }

    @Override
    protected void onScroll(double mx, double my, double amount) {
        if (mx < LIST_W + 4) { listScroll -= (float) amount * ROW * 2; return; }
        if (panelVisible() && mx >= panelX()) { panelScroll -= (float) amount * 20f; return; }
        HudElement e = Iris.hud().elementAt(mx, my);
        if (e == null) e = selected;
        if (e != null) {
            e.setScale(e.scale() + (float) amount * 0.1f);
            selected = e;
        }
    }

    @Override
    protected boolean onKeyDown(int key, int scancode, int modifiers) {
        if (selected != null && panel(selected).capturingInput()) return panel(selected).keyDown(key, scancode, modifiers);
        if (selected == null) return false;
        float step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 5f : 1f;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> nudge(-step, 0);
            case GLFW.GLFW_KEY_RIGHT -> nudge(step, 0);
            case GLFW.GLFW_KEY_UP -> nudge(0, -step);
            case GLFW.GLFW_KEY_DOWN -> nudge(0, step);
            case GLFW.GLFW_KEY_DELETE -> selected.setEnabled(false);
            default -> { return false; }
        }
        return true;
    }

    private void nudge(float dx, float dy) {
        selected.setPosition(selected.x() + dx / vw(), selected.y() + dy / vh());
    }

    @Override
    protected void onChar(int cp) {
        if (selected != null) panel(selected).charTyped(cp);
    }
}
