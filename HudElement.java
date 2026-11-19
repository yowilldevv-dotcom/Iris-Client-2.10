package dev.iris.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.iris.client.config.ConfigManager;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ColorSetting;
import dev.iris.client.setting.SettingGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * One draggable/resizable on-screen information widget. Position is stored as a fraction of the GUI size so layouts
 * survive resolution and GUI-scale changes. Subclasses draw in local coordinates (origin at the element's top-left);
 * the {@link HudManager} applies position and scale.
 *
 * <p>Per-frame order: {@link #prepare(boolean)}, then {@link #width(Draw)} / {@link #height(Draw)}, then
 * {@link #render(Draw, boolean)}. Compute expensive values in {@code prepare} only.
 */
public abstract class HudElement extends SettingGroup {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 3.0f;

    private final double defaultX;
    private final double defaultY;
    private final boolean defaultEnabled;

    private boolean enabled;
    private double x;
    private double y;
    private float scale = 1f;

    // Last drawn bounds in GUI pixels (scaled); used by the HUD editor for hit testing.
    float boundsX, boundsY, boundsW, boundsH;

    public final ColorSetting color = add(new ColorSetting("Color", "Text color.", 0xFFFFFFFF));
    public final BoolSetting shadow = add(new BoolSetting("Shadow", "Draw text with a shadow.", true));
    public final BoolSetting background = add(new BoolSetting("Background", "Draw a translucent panel behind the element.", true));
    public final BoolSetting inWorldOnly = add(new BoolSetting("In world only", "Hide this element on menus and the title screen.", true));

    protected HudElement(String name, String description, double x, double y, boolean enabledByDefault) {
        super(name, description);
        this.defaultX = x;
        this.defaultY = y;
        this.defaultEnabled = enabledByDefault;
        this.x = x;
        this.y = y;
        this.enabled = enabledByDefault;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        ConfigManager.markDirty();
    }

    public double x() { return x; }
    public double y() { return y; }
    public float scale() { return scale; }

    public void setPosition(double fx, double fy) {
        this.x = Math.max(0, Math.min(1, fx));
        this.y = Math.max(0, Math.min(1, fy));
        ConfigManager.markDirty();
    }

    public void setScale(float s) {
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
        ConfigManager.markDirty();
    }

    public float boundsX() { return boundsX; }
    public float boundsY() { return boundsY; }
    public float boundsW() { return boundsW; }
    public float boundsH() { return boundsH; }

    public boolean contains(double px, double py) {
        return px >= boundsX && px <= boundsX + boundsW && py >= boundsY && py <= boundsY + boundsH;
    }

    /** Resets position, scale, visibility and every setting to the element's defaults. */
    public void resetAll() {
        x = defaultX;
        y = defaultY;
        scale = 1f;
        enabled = defaultEnabled;
        resetSettings();
        ConfigManager.markDirty();
    }

    // ---- drawing contract ----

    /** Gather the data to display this frame. {@code preview} is true in the HUD editor (use sample values). */
    public void prepare(boolean preview) {}

    public abstract int width(Draw draw);

    public abstract int height(Draw draw);

    public abstract void render(Draw draw, boolean preview);

    // ---- persistence ----

    public JsonObject save() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("scale", scale);
        o.add("settings", saveSettings());
        return o;
    }

    public List<String> load(JsonObject o) {
        List<String> warnings = new ArrayList<>();
        JsonElement en = o.get("enabled");
        if (en != null && en.isJsonPrimitive() && en.getAsJsonPrimitive().isBoolean()) enabled = en.getAsBoolean();
        Double fx = number(o, "x");
        Double fy = number(o, "y");
        if (fx != null && fy != null) {
            x = Math.max(0, Math.min(1, fx));
            y = Math.max(0, Math.min(1, fy));
        } else if (fx != null || fy != null) {
            warnings.add(name() + ": incomplete position ignored");
        }
        Double sc = number(o, "scale");
        if (sc != null) scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, sc.floatValue()));
        JsonElement settings = o.get("settings");
        if (settings != null && settings.isJsonObject()) warnings.addAll(loadSettings(settings.getAsJsonObject()));
        return warnings;
    }

    private static Double number(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        double d = e.getAsDouble();
        return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
    }
}
