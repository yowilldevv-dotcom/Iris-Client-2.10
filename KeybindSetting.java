package dev.iris.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/**
 * A key binding as a setting (for secondary actions inside a module). The primary toggle key of a module
 * is stored on the module itself. Polling happens through {@link dev.iris.client.util.Input}.
 */
public class KeybindSetting extends Setting<Integer> {
    private boolean wasDown;

    public KeybindSetting(String name, String description, int keyCode) {
        super(name, description, keyCode);
    }

    public int keyCode() { return value; }
    public boolean isBound() { return value >= 0; }

    /** True exactly once per physical key press. Call every tick. */
    public boolean pressedEdge(boolean downNow) {
        boolean edge = downNow && !wasDown;
        wasDown = downNow;
        return edge;
    }

    @Override protected Integer sanitize(Integer candidate) {
        return candidate == null || candidate < 0 ? KeyNames.NONE : candidate;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(KeyNames.name(value)); }

    @Override public void fromJson(JsonElement element) {
        if (!element.isJsonPrimitive()) throw new IllegalArgumentException("expected key name");
        int code = KeyNames.parse(element.getAsString());
        if (code == Integer.MIN_VALUE) throw new IllegalArgumentException("unknown key");
        set(code);
    }

    @Override public String display() { return KeyNames.name(value); }

    @Override public boolean parse(String text) {
        int code = KeyNames.parse(text);
        if (code == Integer.MIN_VALUE) return false;
        set(code);
        return true;
    }

    @Override public List<String> suggestions() { return KeyNames.all(); }

    @Override public KeybindSetting visibleWhen(java.util.function.BooleanSupplier supplier) {
        super.visibleWhen(supplier);
        return this;
    }
}
