package dev.iris.client.hud.elements;

import dev.iris.client.core.Iris;
import dev.iris.client.hud.TextHudElement;

import java.util.List;

/** Client name and version. */
public final class WatermarkElement extends TextHudElement {
    public WatermarkElement() {
        super("Watermark", "Shows the Iris name and version.", 0.01, 0.01, true);
    }

    @Override protected List<String> lines(boolean preview) {
        return List.of("Iris: v" + Iris.version());
    }
}
