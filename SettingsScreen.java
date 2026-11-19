package dev.iris.client.gui;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.SettingGroup;
import dev.iris.client.theme.Theme;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/** Edits the global client preferences and the notification settings. */
public final class SettingsScreen extends IrisScreen {
    private final List<SettingGroup> groups = List.of(Iris.client(), Iris.notificationSettings());
    private final SettingsPanel[] panels = new SettingsPanel[2];
    private int tab;
    private float scroll;
    private float bx, by, bw, bh;

    public SettingsScreen(Screen parent) {
        super("Settings", parent);
        for (int i = 0; i < panels.length; i++) panels[i] = new SettingsPanel(groups.get(i));
    }

    @Override protected void onClosed() { Ui.clearAnims(); }

    private void geometry() {
        bw = Math.max(170f, Math.min(vw() - 16f, 320f));
        bh = Math.max(110f, Math.min(vh() - 16f, 280f));
        bx = (vw() - bw) / 2f;
        by = (vh() - bh) / 2f;
    }

    @Override
    protected void draw(Draw d, float mx, float my, float delta) {
        geometry();
        Theme t = Ui.theme();
        d.rrect(bx, by, bw, bh, 5, Colors.mulAlpha(t.panel(), 0.97f));
        Ui.header(d, bx, by, bw, 22);
        d.text("Settings", bx + 8, by + 7, t.text(), false);
        Ui.button(d, "Back", bx + bw - 44, by + 4, 38, 14, mx, my, true);

        for (int i = 0; i < 2; i++) {
            float tx = bx + 8 + i * 86;
            boolean sel = i == tab;
            Ui.button(d, groups.get(i).name(), tx, by + 26, 82, 14, mx, my, sel);
        }

        float ly = by + 44;
        float lh = bh - 48;
        SettingsPanel p = panels[tab];
        float content = p.layout(bx + 6, ly - scroll, bw - 16);
        float max = Math.max(0, content - lh);
        scroll = Math.max(0, Math.min(max, scroll));
        d.scissor(bx, ly, bw, lh);
        p.layout(bx + 6, ly - scroll, bw - 16);
        p.draw(d, mx, my);
        d.endScissor();
        if (max > 0) {
            float th = Math.max(10f, lh * lh / content);
            d.rect(bx + bw - 5, ly + (lh - th) * (scroll / max), 2, th, Colors.mulAlpha(t.textDim(), 0.7f));
        }
    }

    @Override
    protected void onMouseDown(double mx, double my, int button) {
        if (Ui.hover(mx, my, bx + bw - 44, by + 4, 38, 14)) { onClose(); return; }
        for (int i = 0; i < 2; i++) {
            if (Ui.hover(mx, my, bx + 8 + i * 86, by + 26, 82, 14)) {
                panels[tab].reset();
                tab = i;
                scroll = 0;
                return;
            }
        }
        if (my >= by + 44 && my < by + bh) panels[tab].mouseDown(mx, my, button);
        else panels[tab].reset();
    }

    @Override protected void onMouseDrag(double mx, double my, int button, double dx, double dy) { panels[tab].mouseDrag(mx, my, button); }

    @Override protected void onMouseUp(double mx, double my, int button) { panels[tab].mouseUp(mx, my, button); }

    @Override protected void onScroll(double mx, double my, double amount) { scroll -= (float) amount * 20f; }

    @Override protected boolean onKeyDown(int key, int scancode, int modifiers) { return panels[tab].keyDown(key, scancode, modifiers); }

    @Override protected void onChar(int cp) { panels[tab].charTyped(cp); }
}
