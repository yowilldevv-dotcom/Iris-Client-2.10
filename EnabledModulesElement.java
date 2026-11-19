package dev.iris.client.hud.elements;

import dev.iris.client.core.Iris;
import dev.iris.client.hud.HudElement;
import dev.iris.client.module.Module;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ModeSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** List of enabled modules, longest first, right aligned. */
public final class EnabledModulesElement extends HudElement {
    private final ModeSetting colorMode = add(new ModeSetting("Color mode", "How lines are colored.", "Theme", "Theme", "Rainbow", "Custom"));
    private final BoolSetting showInfo = add(new BoolSetting("Show info", "Append the module's current mode/value.", true));
    private List<String> lines = List.of();
    private int cachedWidth;

    public EnabledModulesElement() {
        super("Enabled Modules", "Lists the modules that are currently enabled.", 0.88, 0.01, true);
    }

    @Override public void prepare(boolean preview) {
        List<String> out = new ArrayList<>();
        if (preview) {
            out.add("Sprint"); out.add("Fullbright"); out.add("Zoom");
        } else {
            for (Module m : Iris.modules().enabled()) {
                if (!m.isShownInList()) continue;
                String info = showInfo.on() ? m.info() : "";
                out.add(info.isEmpty() ? m.name() : m.name() + " " + info);
            }
        }
        lines = out;
    }

    @Override public int width(Draw d) {
        int w = 40;
        for (String s : lines) w = Math.max(w, d.width(s) + 6);
        cachedWidth = w;
        return w;
    }

    @Override public int height(Draw d) { return Math.max(1, lines.size()) * (d.lineHeight() + 2); }

    @Override public void render(Draw d, boolean preview) {
        List<String> sorted = new ArrayList<>(lines);
        sorted.sort(Comparator.comparingInt((String s) -> d.width(s)).reversed());
        int y = 0;
        int i = 0;
        for (String s : sorted) {
            int w = d.width(s) + 6;
            float x = cachedWidth - w;
            if (background.on()) d.rect(x, y, w, d.lineHeight() + 2, Colors.mulAlpha(Iris.themes().current().panel(), 0.7f));
            int c = switch (colorMode.get()) {
                case "Rainbow" -> Colors.rainbow(i * 0.06f, 0.3f);
                case "Custom" -> color.argb();
                default -> Colors.lerp(Iris.themes().current().accent(), Iris.themes().current().accent2(), Math.min(1f, i / 8f));
            };
            d.text(s, x + 3, y + 1, c, shadow.on());
            y += d.lineHeight() + 2;
            i++;
        }
    }
}
