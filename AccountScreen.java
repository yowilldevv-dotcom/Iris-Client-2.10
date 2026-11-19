package dev.iris.client.gui;

import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.theme.Theme;
import dev.iris.client.util.Format;
import dev.iris.client.util.Input;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.management.ManagementFactory;

/**
 * Shows the account information the game already holds for this session (name, UUID, server).
 * It never reads or displays the access token and has no login or password field of any kind.
 */
public final class AccountScreen extends IrisScreen {
    private float bx, by, bw, bh;

    public AccountScreen(Screen parent) { super("Account", parent); }

    @Override protected void onClosed() { Ui.clearAnims(); }

    private String name() {
        var user = Minecraft.getInstance().getUser();
        return user == null ? "Unknown" : user.getName();
    }

    private String uuid() {
        var user = Minecraft.getInstance().getUser();
        return user == null || user.getProfileId() == null ? "Unknown" : user.getProfileId().toString();
    }

    private String location() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip;
        return mc.getSingleplayerServer() != null ? "Singleplayer world" : "Not in a world";
    }

    @Override
    protected void draw(Draw d, float mx, float my, float delta) {
        bw = Math.max(190f, Math.min(vw() - 16f, 330f));
        bh = Math.max(120f, Math.min(vh() - 16f, 170f));
        bx = (vw() - bw) / 2f;
        by = (vh() - bh) / 2f;
        Theme t = Ui.theme();
        d.rrect(bx, by, bw, bh, 5, Colors.mulAlpha(t.panel(), 0.97f));
        Ui.header(d, bx, by, bw, 22);
        d.text("Account & Profile", bx + 8, by + 7, t.text(), false);
        Ui.button(d, "Back", bx + bw - 44, by + 4, 38, 14, mx, my, true);

        String name = name();
        d.rrect(bx + 10, by + 32, 36, 36, 4, t.accent());
        d.textCentered(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(), bx + 28, by + 46, Ui.contrast(t.accent()), false);

        float tx = bx + 54;
        float avail = bw - 64;
        d.textFit(name, tx, by + 34, avail, t.text(), true);
        d.textFit(uuid(), tx, by + 46, avail, t.textDim(), false);
        d.textFit("Playing: " + location(), tx, by + 58, avail, t.textDim(), false);
        long start = ManagementFactory.getRuntimeMXBean().getStartTime();
        d.textFit("Session: " + Format.duration(System.currentTimeMillis() - start), tx, by + 70, avail, t.textDim(), false);

        float btnY = by + 84;
        float half = (bw - 24) / 2f;
        Ui.button(d, "Copy name", bx + 10, btnY, half, 15, mx, my, false);
        Ui.button(d, "Copy UUID", bx + 14 + half, btnY, half, 15, mx, my, false);

        d.rect(bx + 10, by + 106, bw - 20, 1, Colors.mulAlpha(t.outline(), 0.7f));
        float ny = by + 112;
        d.textFit("Iris shows only what the game already knows about this session.", bx + 10, ny, bw - 20, t.success(), false);
        d.textFit("It never reads or stores passwords or session tokens, and nothing here", bx + 10, ny + 11, bw - 20, t.textDim(), false);
        d.textFit("is sent to any external server.", bx + 10, ny + 22, bw - 20, t.textDim(), false);
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (Ui.hover(mx, my, bx + bw - 44, by + 4, 38, 14)) { onClose(); return; }
        float btnY = by + 84;
        float half = (bw - 24) / 2f;
        if (Ui.hover(mx, my, bx + 10, btnY, half, 15)) {
            Input.setClipboard(name());
            dev.iris.client.core.Iris.notifications().success("Account", "Name copied");
        } else if (Ui.hover(mx, my, bx + 14 + half, btnY, half, 15)) {
            Input.setClipboard(uuid());
            dev.iris.client.core.Iris.notifications().success("Account", "UUID copied");
        }
    }
}
