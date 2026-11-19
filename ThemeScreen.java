package dev.iris.client.gui;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.ColorSetting;
import dev.iris.client.theme.Theme;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Theme browser and editor. Clicking a theme applies it immediately. Custom themes can be created (as a copy of the
 * selected theme), renamed, deleted, exported, imported and edited token by token.
 */
public final class ThemeScreen extends IrisScreen {
    private static final float ROW = 15f;
    private static final String[] LABELS = {"New copy", "Rename", "Delete", "Export", "Import"};

    private interface Action { void run() throws IOException; }

    private final TextField nameField = new TextField("Name, or file to import", 64);
    private final TextField tokenField = new TextField("#RRGGBB", 9);
    private String selected = Iris.themes().current().name();
    private String selectedToken;
    private String pendingDelete;
    private long pendingAt;
    private float listScroll;
    private float px, py, pw, ph, lw, listH;

    public ThemeScreen(Screen parent) { super("Themes", parent); }

    @Override protected void onClosed() { Ui.clearAnims(); }

    private Theme selectedTheme() {
        Theme t = Iris.themes().get(selected);
        return t != null ? t : Iris.themes().current();
    }

    private void geometry() {
        pw = Math.max(200f, Math.min(vw() - 16f, 440f));
        ph = Math.max(130f, Math.min(vh() - 16f, 290f));
        px = (vw() - pw) / 2f;
        py = (vh() - ph) / 2f;
        lw = pw * 0.42f;
        listH = ph - 28f - 48f;
    }

    @Override
    protected void draw(Draw d, float mx, float my, float delta) {
        geometry();
        Theme ui = Ui.theme();
        d.rrect(px, py, pw, ph, 5, Colors.mulAlpha(ui.panel(), 0.97f));
        Ui.header(d, px, py, pw, 22);
        d.text("Themes", px + 8, py + 7, ui.text(), false);
        Ui.button(d, "Done", px + pw - 44, py + 4, 38, 14, mx, my, true);

        // theme list
        float lx = px + 8, ly = py + 28;
        d.rrect(lx, ly, lw, listH, 3, ui.background());
        List<Theme> all = Iris.themes().all();
        float max = Math.max(0, all.size() * ROW - listH);
        listScroll = Math.max(0, Math.min(max, listScroll));
        d.scissor(lx, ly, lw, listH);
        float y = ly - listScroll;
        for (Theme t : all) {
            boolean sel = t.name().equals(selected);
            boolean active = t == Iris.themes().current();
            if (sel) d.rect(lx + 1, y, lw - 2, ROW, Colors.mulAlpha(ui.accent(), 0.25f));
            else if (Ui.hover(mx, my, lx, y, lw, ROW) && my >= ly && my < ly + listH) d.rect(lx + 1, y, lw - 2, ROW, Colors.mulAlpha(ui.text(), 0.07f));
            d.rect(lx + 4, y + 4, 6, 7, t.accent());
            d.rect(lx + 10, y + 4, 6, 7, t.accent2());
            d.rect(lx + 16, y + 4, 6, 7, t.panel());
            d.textFit(t.name() + (t.isBuiltin() ? "" : " *"), lx + 27, y + 4, lw - 54, active ? ui.accent() : ui.text(), false);
            if (active) d.textRight("on", lx + lw - 4, y + 4, ui.accent(), false);
            y += ROW;
        }
        d.endScissor();

        // token editor
        float tx = px + 14 + lw;
        float tw = pw - lw - 22;
        Theme sel = selectedTheme();
        if (sel.isBuiltin()) {
            d.textFit("Built-in theme (read only).", tx, ly + 2, tw, ui.textDim(), false);
            d.textFit("Use 'New copy' to make an editable one.", tx, ly + 14, tw, ui.textDim(), false);
        } else {
            float ty = ly;
            float th = 11f;
            for (String token : Theme.TOKENS) {
                boolean isSel = token.equals(selectedToken);
                if (isSel) d.rect(tx - 2, ty, tw + 2, th, Colors.mulAlpha(ui.accent(), 0.22f));
                d.rect(tx, ty + 2, 9, 7, sel.get(token));
                d.outline(tx, ty + 2, 9, 7, ui.outline());
                d.textFit(token, tx + 13, ty + 2, tw - 15, ui.text(), false);
                ty += th;
            }
            if (selectedToken != null) tokenField.draw(d, tx, ty + 3, tw, 15);
        }

        // actions
        float fy = py + ph - 42;
        nameField.draw(d, px + 8, fy, pw - 16, 15);
        float bw = (pw - 16 - 4 * 3) / 5f;
        for (int i = 0; i < LABELS.length; i++) {
            String label = i == 2 && pendingDelete != null && System.currentTimeMillis() - pendingAt < 4000 ? "Confirm?" : LABELS[i];
            Ui.button(d, label, px + 8 + i * (bw + 3), fy + 19, bw, 15, mx, my, false);
        }
    }

    private void run(String what, Action a) {
        try {
            a.run();
        } catch (IOException | RuntimeException e) {
            Iris.notifications().error(what + " failed", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private Path exportDir() { return Iris.config().root().resolve("exports"); }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (Ui.hover(mx, my, px + pw - 44, py + 4, 38, 14)) { onClose(); return; }
        nameField.setFocused(false);
        float lx = px + 8, ly = py + 28;
        float fy = py + ph - 42;

        if (Ui.hover(mx, my, lx, ly, lw, listH)) {
            int idx = (int) ((my - ly + listScroll) / ROW);
            List<Theme> all = Iris.themes().all();
            if (idx >= 0 && idx < all.size()) {
                selected = all.get(idx).name();
                Iris.themes().setCurrent(selected);
                selectedToken = null;
                tokenField.setFocused(false);
            }
            return;
        }
        if (Ui.hover(mx, my, px + 8, fy, pw - 16, 15)) { nameField.setFocused(true); tokenField.setFocused(false); return; }

        float bw = (pw - 16 - 4 * 3) / 5f;
        for (int i = 0; i < LABELS.length; i++) {
            if (Ui.hover(mx, my, px + 8 + i * (bw + 3), fy + 19, bw, 15)) { button(i); return; }
        }

        Theme sel = selectedTheme();
        if (!sel.isBuiltin()) {
            float tx = px + 14 + lw;
            float tw = pw - lw - 22;
            float th = 11f;
            for (int i = 0; i < Theme.TOKENS.size(); i++) {
                if (Ui.hover(mx, my, tx - 2, ly + i * th, tw + 2, th)) {
                    selectedToken = Theme.TOKENS.get(i);
                    tokenField.setText(ColorSetting.hex(sel.get(selectedToken)));
                    tokenField.setFocused(true);
                    return;
                }
            }
            float ty = ly + Theme.TOKENS.size() * th;
            tokenField.setFocused(selectedToken != null && Ui.hover(mx, my, tx, ty + 3, tw, 15));
        }
    }

    private void button(int i) {
        String name = nameField.text().trim();
        Theme sel = selectedTheme();
        switch (i) {
            case 0 -> run("Create theme", () -> {
                if (name.isEmpty()) throw new IOException("Type a name for the new theme first.");
                Theme t = Iris.themes().create(name, sel);
                selected = t.name();
                Iris.themes().setCurrent(t.name());
                nameField.clear();
            });
            case 1 -> run("Rename theme", () -> {
                if (name.isEmpty()) throw new IOException("Type the new name first.");
                String old = sel.name();
                Iris.themes().rename(old, name);
                selected = name;
                nameField.clear();
            });
            case 2 -> {
                if (pendingDelete != null && pendingDelete.equals(sel.name()) && System.currentTimeMillis() - pendingAt < 4000) {
                    pendingDelete = null;
                    run("Delete theme", () -> {
                        Iris.themes().delete(sel.name());
                        selected = Iris.themes().current().name();
                    });
                } else {
                    pendingDelete = sel.name();
                    pendingAt = System.currentTimeMillis();
                }
            }
            case 3 -> run("Export theme", () -> {
                Path dest = exportDir().resolve(sel.name().replace(' ', '_') + ".theme.json");
                Iris.themes().exportTo(sel.name(), dest);
                Iris.notifications().success("Theme exported", dest.getFileName().toString());
            });
            case 4 -> run("Import theme", () -> {
                if (name.isEmpty()) throw new IOException("Type a file path (or a file name inside the exports folder).");
                Path p = Path.of(name);
                if (!p.isAbsolute()) {
                    p = exportDir().resolve(name.endsWith(".json") ? name : name + ".json").normalize();
                    if (!p.startsWith(exportDir().normalize())) throw new IOException("Relative paths must stay inside the exports folder.");
                }
                Theme t = Iris.themes().importFrom(p);
                selected = t.name();
                Iris.notifications().success("Theme imported", t.name());
                nameField.clear();
            });
            default -> { }
        }
    }

    private void applyToken() {
        Theme sel = selectedTheme();
        if (sel.isBuiltin() || selectedToken == null) return;
        Integer c = ColorSetting.parseColor(tokenField.text());
        if (c == null) {
            Iris.notifications().error("Invalid color", "Use #RRGGBB or #AARRGGBB");
            return;
        }
        sel.set(selectedToken, c);
        run("Save theme", () -> Iris.themes().save(sel));
    }

    @Override protected void onScroll(double mx, double my, double amount) { listScroll -= (float) amount * ROW * 2; }

    @Override
    protected boolean onKeyDown(int key, int scancode, int modifiers) {
        if (tokenField.isFocused()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { applyToken(); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { tokenField.setFocused(false); return true; }
            return tokenField.onKey(key, modifiers) || true;
        }
        if (nameField.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { nameField.setFocused(false); return true; }
            return nameField.onKey(key, modifiers) || key == GLFW.GLFW_KEY_ENTER;
        }
        return false;
    }

    @Override
    protected void onChar(int cp) {
        tokenField.onChar(cp);
        nameField.onChar(cp);
    }
}
