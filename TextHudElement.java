package dev.iris.client.hud;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.BoolSetting;

import java.util.List;

/**
 * Convenience base for elements that show one or more lines of text. A line of the form "Label: value" is drawn with
 * the label in the theme accent color (optional) and the value in the element color.
 */
public abstract class TextHudElement extends HudElement {
    protected static final int PAD = 3;

    public final BoolSetting accentLabels = add(new BoolSetting("Accent labels", "Draw 'Label:' in the theme accent color.", true));

    private List<String> cached = List.of();

    protected TextHudElement(String name, String description, double x, double y, boolean enabledByDefault) {
        super(name, description, x, y, enabledByDefault);
    }

    /** Lines to display. Called once per frame from {@link #prepare(boolean)}. Never return null. */
    protected abstract List<String> lines(boolean preview);

    @Override public void prepare(boolean preview) {
        List<String> l = lines(preview);
        cached = l == null ? List.of() : l;
    }

    @Override public int width(Draw d) {
        int w = 0;
        for (String s : cached) w = Math.max(w, d.width(s));
        return w + PAD * 2;
    }

    @Override public int height(Draw d) {
        return Math.max(1, cached.size()) * (d.lineHeight() + 1) + PAD * 2 - 1;
    }

    @Override public void render(Draw d, boolean preview) {
        if (background.on()) {
            d.rrect(0, 0, width(d), height(d), 3, Colors.mulAlpha(Iris.themes().current().panel(), 0.75f));
        }
        int y = PAD;
        int accent = Iris.themes().current().accent();
        for (String line : cached) {
            int sep = accentLabels.on() ? line.indexOf(": ") : -1;
            if (sep > 0) {
                String label = line.substring(0, sep + 1);
                d.text(label, PAD, y, accent, shadow.on());
                d.text(line.substring(sep + 1), PAD + d.width(label), y, color.argb(), shadow.on());
            } else {
                d.text(line, PAD, y, color.argb(), shadow.on());
            }
            y += d.lineHeight() + 1;
        }
    }
}
