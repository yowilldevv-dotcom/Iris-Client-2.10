package dev.iris.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.iris.client.config.JsonFiles;
import dev.iris.client.core.Iris;
import dev.iris.client.module.Category;
import dev.iris.client.module.Module;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.KeyNames;
import dev.iris.client.theme.Theme;
import dev.iris.client.util.Mc;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The main ClickGUI: one draggable window per module category, a search box, per-module settings panels and
 * keybind capture. Window positions persist in {@code config/irisclient/gui.json}.
 */
public final class ClickGuiScreen extends IrisScreen {
    private static final float WIN_W = 118f;
    private static final float HEAD = 16f;
    private static final float ROW = 14f;
    private static final float BAR = 26f;

    /** Remembered window state (x, y, open) so positions survive reopening even if the file is unwritable. */
    private static final Map<Category, float[]> POS = new EnumMap<>(Category.class);
    private static boolean loaded;

    private static final class Win {
        Category cat;
        float x, y, scroll;
        boolean open = true;
        float bodyY, bodyH;
    }

    private record Hit(float x, float y, float w, float h, float cx, float cy, float cw, float ch,
                       Win win, Runnable left, Runnable right, SettingsPanel panel) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h && mx >= cx && mx < cx + cw && my >= cy && my < cy + ch;
        }
    }

    private final List<Win> wins = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final Map<Module, SettingsPanel> panels = new HashMap<>();
    private final Set<Module> expanded = new HashSet<>();
    private final TextField search = new TextField("Search modules...", 32);
    private float searchX, searchW;
    private Win dragWin;
    private float dragDx, dragDy;
    private SettingsPanel activePanel;
    private Module binding;
    private Module hoverModule;

    public ClickGuiScreen(Screen parent) {
        super("ClickGUI", parent);
        loadPositions();
        for (Category c : Category.values()) {
            Win w = new Win();
            w.cat = c;
            float[] p = POS.get(c);
            w.x = p[0];
            w.y = p[1];
            w.open = p[2] > 0.5f;
            wins.add(w);
        }
    }

    // ---- persistence ----

    private static Path file() { return Iris.config().root().resolve("gui.json"); }

    private static void loadPositions() {
        if (loaded) return;
        loaded = true;
        int i = 0;
        for (Category c : Category.values()) {
            POS.put(c, new float[]{8f + i * (WIN_W + 6f), BAR + 8f, 1f});
            i++;
        }
        try {
            if (!Files.exists(file())) return;
            JsonObject o = JsonFiles.read(file());
            for (Category c : Category.values()) {
                JsonElement e = o.get(c.name());
                if (e == null || !e.isJsonObject()) continue;
                JsonObject w = e.getAsJsonObject();
                float[] p = POS.get(c);
                if (w.has("x") && w.get("x").isJsonPrimitive()) p[0] = w.get("x").getAsFloat();
                if (w.has("y") && w.get("y").isJsonPrimitive()) p[1] = w.get("y").getAsFloat();
                if (w.has("open") && w.get("open").isJsonPrimitive()) p[2] = w.get("open").getAsBoolean() ? 1f : 0f;
            }
        } catch (IOException | RuntimeException e) {
            Iris.LOGGER.warn("Could not read gui.json, using default window layout: {}", e.getMessage());
        }
    }

    private void savePositions() {
        JsonObject o = new JsonObject();
        for (Win w : wins) {
            POS.put(w.cat, new float[]{w.x, w.y, w.open ? 1f : 0f});
            JsonObject j = new JsonObject();
            j.addProperty("x", w.x);
            j.addProperty("y", w.y);
            j.addProperty("open", w.open);
            o.add(w.cat.name(), j);
        }
        try {
            JsonFiles.write(file(), o);
        } catch (IOException e) {
            Iris.LOGGER.warn("Could not save gui.json: {}", e.getMessage());
        }
    }

    @Override protected void onClosed() {
        savePositions();
        Ui.clearAnims();
    }

    // ---- drawing ----

    private SettingsPanel panel(Module m) { return panels.computeIfAbsent(m, SettingsPanel::new); }

    private void hit(float x, float y, float w, float h, float[] clip, Win win, Runnable l, Runnable r, SettingsPanel p) {
        hits.add(new Hit(x, y, w, h, clip[0], clip[1], clip[2], clip[3], win, l, r, p));
    }

    @Override
    protected void draw(Draw d, float mx, float my, float delta) {
        hits.clear();
        hoverModule = null;
        Theme t = Ui.theme();
        String q = search.text().trim();
        Set<Module> matches = q.isEmpty() ? null : new HashSet<>(Iris.modules().search(q));

        for (Win w : wins) clampWindow(w);
        for (Win w : wins) drawWindow(d, w, mx, my, matches);
        drawBar(d, mx, my);

        if (hoverModule != null && dragWin == null) {
            String desc = hoverModule.description();
            float tw = Math.min(vw() - 12, d.width(desc) + 10);
            float tx = Math.min(vw() - tw - 4, mx + 10);
            float ty = Math.min(vh() - 16, my + 12);
            d.rrect(tx, ty, tw, 14, 3, Colors.withAlpha(t.panelAlt(), 245));
            d.outline(tx, ty, tw, 14, Colors.mulAlpha(t.accent(), 0.6f));
            d.textFit(desc, tx + 5, ty + 3, tw - 8, t.text(), false);
        }
    }

    private void clampWindow(Win w) {
        w.x = Math.max(0, Math.min(vw() - WIN_W, w.x));
        w.y = Math.max(0, Math.min(vh() - HEAD, w.y));
    }

    private void drawBar(Draw d, float mx, float my) {
        Theme t = Ui.theme();
        float w = vw();
        d.rect(0, 0, w, BAR, Colors.withAlpha(t.header(), 245));
        Screens.accentBar(d, 0, BAR - 2, w, 2);
        Screens.drawLogoMark(d, 15, BAR / 2f - 1, 8);
        Screens.gradientText(d, "Iris", 28, 6, 1.5f);
        d.text("v" + Iris.version(), 28 + d.width("Iris") * 1.5f + 4, 14, t.textDim(), false);

        boolean wide = w >= 470f;
        String[] labels = wide ? new String[]{"Settings", "Account", "Themes", "Configs", "HUD"} : new String[]{"Settings", "Configs", "HUD"};
        float bw = 44f;
        float x = w - 6f;
        for (String label : labels) {
            x -= bw;
            float bx = x;
            Ui.button(d, label, bx, 5, bw, 16, mx, my, false);
            hit(bx, 5, bw, 16, new float[]{0, 0, w, BAR}, null, () -> openSub(label), null, null);
            x -= 3f;
        }
        float cycleW = 64f;
        x -= cycleW;
        float cx = x;
        Ui.button(d, Iris.themes().current().name(), cx, 5, cycleW, 16, mx, my, true);
        hit(cx, 5, cycleW, 16, new float[]{0, 0, w, BAR}, null, () -> cycleTheme(1), () -> cycleTheme(-1), null);

        searchX = 100f;
        searchW = Math.max(50f, Math.min(150f, x - 8f - searchX));
        search.draw(d, searchX, 5, searchW, 16);
    }

    private void openSub(String label) {
        switch (label) {
            case "Settings" -> Mc.openScreen(Screens.settings());
            case "Account" -> Mc.openScreen(Screens.account());
            case "Themes" -> Mc.openScreen(Screens.themes());
            case "Configs" -> Mc.openScreen(Screens.configs());
            case "HUD" -> Mc.openScreen(Screens.hudEditor());
            default -> { }
        }
    }

    private void cycleTheme(int dir) {
        List<String> names = Iris.themes().names();
        int i = names.indexOf(Iris.themes().current().name());
        Iris.themes().setCurrent(names.get(((i + dir) % names.size() + names.size()) % names.size()));
    }

    private float contentHeight(List<Module> mods) {
        float h = 0;
        for (Module m : mods) {
            h += ROW;
            if (expanded.contains(m)) h += ROW + panel(m).layout(0, 0, WIN_W - 4) + 3;
        }
        return h;
    }

    private void drawWindow(Draw d, Win w, float mx, float my, Set<Module> matches) {
        Theme t = Ui.theme();
        List<Module> mods = new ArrayList<>();
        for (Module m : Iris.modules().byCategory(w.cat)) if (matches == null || matches.contains(m)) mods.add(m);
        if (matches != null && mods.isEmpty()) return;

        float content = contentHeight(mods);
        float maxBody = Math.max(40f, vh() - w.y - HEAD - 6f);
        float bodyH = w.open ? Math.min(content + 2, maxBody) : 0;
        w.bodyY = w.y + HEAD;
        w.bodyH = bodyH;
        float maxScroll = Math.max(0, content + 2 - bodyH);
        w.scroll = Math.max(0, Math.min(maxScroll, w.scroll));

        if (w.open) d.rrect(w.x, w.y, WIN_W, HEAD + bodyH, 4, Colors.mulAlpha(t.panel(), 0.97f));
        Ui.header(d, w.x, w.y, WIN_W, HEAD);
        d.text(w.cat.displayName(), w.x + 6, w.y + 4, t.text(), false);
        d.textRight(w.open ? "-" : "+", w.x + WIN_W - 6, w.y + 4, t.textDim(), false);
        float[] full = {0, 0, vw(), vh()};
        hit(w.x, w.y, WIN_W, HEAD, full, w, () -> { dragWin = w; dragDx = 0; dragDy = 0; },
                () -> { w.open = !w.open; savePositions(); }, null);
        if (!w.open) return;

        float[] clip = {w.x, w.bodyY, WIN_W, bodyH};
        d.scissor(w.x, w.bodyY, WIN_W, bodyH);
        float y = w.bodyY + 1 - w.scroll;
        for (Module m : mods) {
            boolean on = m.isEnabled();
            boolean over = Ui.hover(mx, my, w.x, y, WIN_W, ROW) && Ui.hover(mx, my, clip[0], clip[1], clip[2], clip[3]);
            float a = Ui.anim("mod:" + m.id(), over);
            float e = Ui.anim("on:" + m.id(), on);
            if (e > 0.01f) d.rect(w.x + 1, y, WIN_W - 2, ROW, Colors.mulAlpha(t.accent(), 0.25f * e));
            if (a > 0.01f) d.rect(w.x + 1, y, WIN_W - 2, ROW, Colors.mulAlpha(t.text(), 0.07f * a));
            if (e > 0.01f) d.rect(w.x + 1, y + 1, 2, ROW - 2, Colors.mulAlpha(t.accent(), e));
            d.textFit(m.name(), w.x + 7, y + 3, WIN_W - 24, on ? t.accent() : t.text(), false);
            d.textRight(expanded.contains(m) ? "v" : ">", w.x + WIN_W - 5, y + 3, t.textDim(), false);
            if (over) hoverModule = m;
            hit(w.x, y, WIN_W, ROW, clip, w, m::toggle, () -> toggleExpanded(m), null);
            y += ROW;
            if (expanded.contains(m)) {
                String key = binding == m ? "press a key..." : KeyNames.name(m.key());
                d.text("Keybind", w.x + 7, y + 3, t.text(), false);
                d.textRight(key, w.x + WIN_W - 5, y + 3, binding == m ? t.warning() : t.accent(), false);
                hit(w.x, y, WIN_W, ROW, clip, w, () -> binding = m, () -> { m.setKey(-1); binding = null; }, null);
                y += ROW;
                SettingsPanel p = panel(m);
                float ph = p.layout(w.x + 2, y, WIN_W - 4);
                p.draw(d, mx, my);
                hit(w.x + 2, y, WIN_W - 4, ph, clip, w, null, null, p);
                y += ph + 3;
            }
        }
        d.endScissor();
        if (maxScroll > 0) {
            float th = Math.max(8f, bodyH * bodyH / (content + 2));
            float ty = w.bodyY + (bodyH - th) * (w.scroll / maxScroll);
            d.rect(w.x + WIN_W - 2, ty, 1.5f, th, Colors.mulAlpha(t.textDim(), 0.7f));
        }
    }

    private void toggleExpanded(Module m) {
        if (!expanded.remove(m)) expanded.add(m);
        panel(m).reset();
        if (binding == m) binding = null;
    }

    // ---- input ----

    private Win windowAt(double mx, double my) {
        for (int i = wins.size() - 1; i >= 0; i--) {
            Win w = wins.get(i);
            float h = HEAD + (w.open ? w.bodyH : 0);
            if (mx >= w.x && mx < w.x + WIN_W && my >= w.y && my < w.y + h) return w;
        }
        return null;
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        binding = null;
        boolean inSearch = mx >= searchX && mx < searchX + searchW && my >= 5 && my < 21;
        search.setFocused(inSearch);
        if (inSearch) return;

        Win top = my < BAR ? null : windowAt(mx, my);
        if (top != null) {
            wins.remove(top);
            wins.add(top);
        }
        SettingsPanel panelHit = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.win() != top || !h.contains(mx, my)) continue;
            if (h.panel() != null) {
                panelHit = h.panel();
                break;
            }
            Runnable r = button == 1 ? h.right() : button == 0 ? h.left() : null;
            if (r != null) {
                if (r == h.left() && h.win() != null && h.y() < h.win().y + HEAD + 0.5f && h.h() == HEAD) {
                    dragWin = h.win();
                    dragDx = (float) mx - dragWin.x;
                    dragDy = (float) my - dragWin.y;
                } else {
                    r.run();
                }
            }
            break;
        }
        for (SettingsPanel p : panels.values()) if (p != panelHit) p.reset();
        if (panelHit != null) {
            activePanel = panelHit;
            panelHit.mouseDown(mx, my, button);
        } else {
            activePanel = null;
        }
    }

    @Override
    protected void onMouseDrag(double mx, double my, int button, double dx, double dy) {
        if (dragWin != null) {
            dragWin.x = (float) mx - dragDx;
            dragWin.y = (float) my - dragDy;
        } else if (activePanel != null) {
            activePanel.mouseDrag(mx, my, button);
        }
    }

    @Override
    protected void onMouseUp(double mx, double my, int button) {
        if (dragWin != null) {
            dragWin = null;
            savePositions();
        }
        for (SettingsPanel p : panels.values()) p.mouseUp(mx, my, button);
    }

    @Override
    protected void onScroll(double mx, double my, double amount) {
        Win w = windowAt(mx, my);
        if (w != null && w.open) w.scroll -= (float) amount * ROW * 2;
    }

    @Override
    protected boolean onKeyDown(int key, int scancode, int modifiers) {
        if (binding != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) binding.setKey(-1);
            else if (key != GLFW.GLFW_KEY_ESCAPE) binding.setKey(key);
            binding = null;
            return true;
        }
        if (activePanel != null && activePanel.capturingInput()) return activePanel.keyDown(key, scancode, modifiers);
        if (search.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (search.text().isEmpty()) return false;
                search.clear();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) { search.setFocused(false); return true; }
            return search.onKey(key, modifiers) || true;
        }
        return false;
    }

    @Override
    protected void onChar(int cp) {
        if (activePanel != null && activePanel.capturingInput()) activePanel.charTyped(cp);
        else search.onChar(cp);
    }
}
