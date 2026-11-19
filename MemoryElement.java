package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.ModeSetting;

import java.util.List;

/** JVM memory use. */
public final class MemoryElement extends TextHudElement {
    private final ModeSetting mode = add(new ModeSetting("Mode", "How memory is shown.", "Used / Max", "Used / Max", "Percent"));
    private long lastRead;
    private long used;
    private long max = 1;

    public MemoryElement() {
        super("Memory", "Shows Java memory usage.", 0.01, 0.19, false);
    }

    @Override protected List<String> lines(boolean preview) {
        long now = System.currentTimeMillis();
        if (now - lastRead > 500) {
            Runtime rt = Runtime.getRuntime();
            used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            max = Math.max(1, rt.maxMemory() / (1024 * 1024));
            lastRead = now;
        }
        if (mode.is("Percent")) return List.of("Memory: " + Math.round(used * 100.0 / max) + "%");
        return List.of("Memory: " + used + " / " + max + " MB");
    }
}
