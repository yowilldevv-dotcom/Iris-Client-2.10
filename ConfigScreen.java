package dev.iris.client.gui;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.theme.Theme;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Profile manager: lists configuration profiles and offers load, save, create, duplicate, rename, delete,
 * export and import. Every failure is reported as an error notification.
 */
public final class ConfigScreen extends IrisScreen {
    private static final float ROW = 16f;
    private static final String[] LABELS = {"Load", "Save active", "New", "Duplicate", "Rename", "Delete", "Export", "Import"};

    private interface IoAction { void run() throws IOException; }

    private List<String> profiles = List.of();
    private String selected;
    private float scroll;
    private final TextField field = new TextField("Name, or file path / exports file to import", 160);
    private String pendingDelete;
    private long pendingAt;

    // layout (virtual pixels), recomputed by layout()
    private float px, py, pw, ph;
    private float lx, ly, lw, lh;
    private float fx, fy, fw;
    private float bx, by, bw;
    private int cols;

    public ConfigScreen(Screen parent) { super("Configs", parent); }

    @Override
    protected void onOpen() { refresh(); }

    @Override
    protected void onClosed() { Ui.clearAnims(); }

    private void refresh() {
        profiles = Iris.config().list();
        if (selected == null || indexOf(selected) < 0) selected = Iris.config().activeName();
        if (indexOf(selected) < 0) selected = profiles.isEmpty() ? null : profiles.get(0);
        clampScroll();
    }

    private int indexOf(String name) {
        if (name == null) return -1;
        for (int i = 0; i < profiles.size(); i++) if (profiles.get(i).equalsIgnoreCase(name)) return i;
        return -1;
    }

    private void select(String name) {
        int i = indexOf(name);
        selected = i >= 0 ? profiles.get(i) : selected;
    }

    private void layout() {
        pw = Math.max(180f, Math.min(vw() - 16f, 440f));
        ph = Math.max(120f, Math.min(vh() - 16f, 300f));
        px = (vw() - pw) / 2f;
        py = (vh() - ph) / 2f;
        float inner = pw - 16f;
        cols = inner >= 330f ? 4 : 2;
        int rows = (LABELS.length + cols - 1) / cols;
        bw = (inner - (cols - 1) * 3f) / cols;
        bx = px + 8f;
        by = py + ph - 8f - rows * 19f + 3f;
        fx = px + 8f;
        fw = inner;
        fy = by - 22f;
        lx = px + 8f;
        lw = inner;
        ly = py + 28f;
        lh = Math.max(20f, fy - 4f - ly);
    }

    private float maxScroll() { return Math.max(0f, profiles.size() * ROW - lh); }

    private void clampScroll() { scroll = Math.max(0f, Math.min(scroll, maxScroll())); }

    private String buttonLabel(int i) {
        if (i == 5 && pendingDelete != null && System.currentTimeMillis() - pendingAt < 4000) return "Confirm?";
        return LABELS[i];
    }

    @Override
    protected void draw(Draw d, float mx, float my, float delta) {
        layout();
        clampScroll();
        Theme t = Ui.theme();
        d.rrect(px, py, pw, ph, 5, Colors.mulAlpha(t.panel(), 0.96f));
        Ui.header(d, px, py, pw, 22);
        d.textFit("Configs  -  active: " + Iris.config().activeName(), px + 8, py + (22 - d.lineHeight()) / 2f + 1, pw - 70, t.text(), false);
        Ui.button(d, "Done", px + pw - 46, py + 4, 40, 14, mx, my, true);

        d.rrect(lx, ly, lw, lh, 3, t.background());
        d.scissor(lx, ly, lw, lh);
        if (profiles.isEmpty()) d.text("No profiles found.", lx + 6, ly + 5, t.textDim(), false);
        String active = Iris.config().activeName();
        for (int i = 0; i < profiles.size(); i++) {
            float y = ly + i * ROW - scroll;
            if (y + ROW < ly || y > ly + lh) continue;
            String name = profiles.get(i);
            boolean sel = name.equals(selected);
            boolean isActive = name.equals(active);
            boolean over = Ui.hover(mx, my, lx, ly, lw, lh) && Ui.hover(mx, my, lx, y, lw, ROW);
            if (sel) d.rect(lx + 1, y, lw - 2, ROW, Colors.mulAlpha(t.accent(), 0.28f));
            else if (over) d.rect(lx + 1, y, lw - 2, ROW, Colors.mulAlpha(t.accent(), 0.12f));
            if (isActive) d.rect(lx + 1, y + 2, 2, ROW - 4, t.accent());
            float ty = y + (ROW - d.lineHeight()) / 2f + 1;
            float right = lx + lw - 8;
            if (isActive) {
                d.textRight("active", right, ty, t.accent(), false);
                right -= d.width("active") + 8;
            }
            d.textFit(name, lx + 8, ty, Math.max(10, right - lx - 8), isActive ? t.accent() : t.text(), false);
        }
        d.endScissor();
        if (maxScroll() > 0) {
            float th = Math.max(10f, lh * lh / (profiles.size() * ROW));
            float ty = ly + (lh - th) * (scroll / maxScroll());
            d.rect(lx + lw - 3, ty, 2, th, Colors.mulAlpha(t.textDim(), 0.7f));
        }

        field.draw(d, fx, fy, fw, 18);
        for (int i = 0; i < LABELS.length; i++) {
            float x = bx + (i % cols) * (bw + 3f);
            float y = by + (i / cols) * 19f;
            Ui.button(d, buttonLabel(i), x, y, bw, 16, mx, my, i == 0);
        }
    }

    @Override
    protected void onMouseDown(double x, double y, int button) {
        layout();
        if (button != 0) return;
        if (Ui.hover(x, y, px + pw - 46, py + 4, 40, 14)) { onClose(); return; }
        field.setFocused(Ui.hover(x, y, fx, fy, fw, 18));
        if (Ui.hover(x, y, lx, ly, lw, lh)) {
            int i = (int) ((y - ly + scroll) / ROW);
            if (i >= 0 && i < profiles.size() && !profiles.get(i).equals(selected)) {
                selected = profiles.get(i);
                pendingDelete = null;
            }
            return;
        }
        for (int i = 0; i < LABELS.length; i++) {
            float bxx = bx + (i % cols) * (bw + 3f);
            float byy = by + (i / cols) * 19f;
            if (Ui.hover(x, y, bxx, byy, bw, 16)) { act(i); return; }
        }
    }

    @Override
    protected void onScroll(double x, double y, double amount) {
        layout();
        scroll -= (float) amount * ROW;
        clampScroll();
    }

    @Override
    protected boolean onKeyDown(int key, int scancode, int modifiers) {
        if (!field.isFocused()) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            field.setFocused(false);
            return true;
        }
        return field.onKey(key, modifiers);
    }

    @Override
    protected void onChar(int codePoint) { field.onChar(codePoint); }

    // ---- actions ----

    private void act(int i) {
        String name = field.text().trim();
        if (i != 5) pendingDelete = null;
        switch (i) {
            case 0 -> {
                if (!requireSelection()) return;
                run("Load", () -> {
                    List<String> warnings = Iris.config().load(selected);
                    if (!warnings.isEmpty()) {
                        Iris.notifications().warn("Config loaded", warnings.size() + " issue(s): " + warnings.get(0));
                    }
                });
            }
            case 1 -> run("Save", () -> {
                Iris.config().saveActive();
                Iris.notifications().success("Config saved", Iris.config().activeName());
            });
            case 2 -> {
                if (!requireName(name)) return;
                run("New", () -> {
                    Iris.config().create(name);
                    refresh();
                    select(name);
                    field.clear();
                    Iris.notifications().success("Config created", name);
                });
            }
            case 3 -> {
                if (!requireSelection() || !requireName(name)) return;
                run("Duplicate", () -> {
                    Iris.config().duplicate(selected, name);
                    refresh();
                    select(name);
                    field.clear();
                    Iris.notifications().success("Config duplicated", name);
                });
            }
            case 4 -> {
                if (!requireSelection() || !requireName(name)) return;
                run("Rename", () -> {
                    Iris.config().rename(selected, name);
                    refresh();
                    select(name);
                    field.clear();
                    Iris.notifications().success("Config renamed", name);
                });
            }
            case 5 -> {
                if (!requireSelection()) return;
                long now = System.currentTimeMillis();
                if (selected.equals(pendingDelete) && now - pendingAt < 4000) {
                    pendingDelete = null;
                    run("Delete", () -> {
                        Iris.config().delete(selected);
                        Iris.notifications().success("Config deleted", selected);
                    });
                } else {
                    pendingDelete = selected;
                    pendingAt = now;
                    Iris.notifications().warn("Delete '" + selected + "'?", "Click Delete again within 4 seconds to confirm.");
                }
            }
            case 6 -> {
                if (!requireSelection()) return;
                run("Export", () -> {
                    String file = selected.replaceAll("[\\\\/:*?\"<>|]", "_") + ".json";
                    Path dest = exportsDir().resolve(file);
                    Iris.config().exportTo(selected, dest);
                    Iris.notifications().success("Config exported", "config/irisclient/exports/" + file);
                });
            }
            case 7 -> {
                if (!requireName(name)) return;
                run("Import", () -> {
                    String imported = Iris.config().importFrom(resolveImport(name), null);
                    refresh();
                    select(imported);
                    field.clear();
                    Iris.notifications().success("Config imported", imported);
                });
            }
            default -> { }
        }
    }

    private Path exportsDir() { return Iris.config().root().resolve("exports"); }

    /** A bare name refers to the exports folder; anything with a path separator is used as given. */
    private Path resolveImport(String text) {
        String s = text;
        if (s.length() > 1 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        Path p = Path.of(s);
        if (p.isAbsolute() || s.contains("/") || s.contains("\\")) return p;
        Path inExports = exportsDir().resolve(s);
        if (Files.isRegularFile(inExports)) return inExports;
        Path withExt = exportsDir().resolve(s + ".json");
        return Files.isRegularFile(withExt) ? withExt : inExports;
    }

    private boolean requireSelection() {
        if (selected != null) return true;
        Iris.notifications().error("No config selected", "Click a profile in the list first.");
        return false;
    }

    private boolean requireName(String name) {
        if (!name.isEmpty()) return true;
        Iris.notifications().error("Name required", "Type a name or file path in the field first.");
        return false;
    }

    private void run(String what, IoAction action) {
        try {
            action.run();
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            Iris.notifications().error(what + " failed", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            refresh();
        }
    }
}
