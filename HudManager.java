package dev.iris.client.hud;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Draw;
import dev.iris.client.util.Mc;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Registry and renderer for HUD elements. Exceptions in one element never break the others. */
public final class HudManager {
    private final List<HudElement> elements = new ArrayList<>();
    private final Map<String, HudElement> byId = new HashMap<>();

    public void register(HudElement... list) {
        for (HudElement e : list) {
            if (byId.putIfAbsent(e.id(), e) != null) throw new IllegalStateException("Duplicate HUD element: " + e.name());
            elements.add(e);
        }
    }

    public List<HudElement> all() { return List.copyOf(elements); }

    public HudElement get(String id) {
        return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT).replace(' ', '-'));
    }

    /**
     * Draws all enabled elements.
     *
     * @param editing true inside the HUD editor: elements show sample data and ignore the in-world-only filter
     */
    public void render(Draw d, int sw, int sh, boolean editing) {
        Minecraft mc = Minecraft.getInstance();
        if (!editing) {
            if (!Iris.client().hudVisible.on()) return;
            if (mc.gui.hud.isHidden()) return;
        }
        boolean inWorld = Mc.inWorld();
        for (HudElement e : elements) {
            if (!e.isEnabled()) continue;
            if (!editing && e.inWorldOnly.on() && !inWorld) continue;
            try {
                e.prepare(editing || !inWorld);
                int w = e.width(d);
                int h = e.height(d);
                float s = e.scale();
                float bw = w * s;
                float bh = h * s;
                float px = (float) Math.max(0, Math.min(sw - bw, e.x() * sw));
                float py = (float) Math.max(0, Math.min(sh - bh, e.y() * sh));
                e.boundsX = px;
                e.boundsY = py;
                e.boundsW = bw;
                e.boundsH = bh;
                d.pushScaled(px, py, s);
                try {
                    e.render(d, editing || !inWorld);
                } finally {
                    d.pop();
                }
            } catch (RuntimeException ex) {
                Iris.LOGGER.warn("HUD element {} failed to render", e.name(), ex);
            }
        }
    }

    /** Topmost enabled element under the point (later elements draw on top), or null. */
    public HudElement elementAt(double px, double py) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement e = elements.get(i);
            if (e.isEnabled() && e.contains(px, py)) return e;
        }
        return null;
    }

    public void resetAll() {
        for (HudElement e : elements) e.resetAll();
    }
}
