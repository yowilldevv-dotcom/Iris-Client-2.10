package dev.iris.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Locale;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean def) {
        super(name, description, def);
    }

    public boolean on() { return value; }
    public void toggle() { set(!value); }

    @Override protected Boolean sanitize(Boolean candidate) { return candidate != null && candidate; }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("expected boolean");
        }
        set(element.getAsBoolean());
    }

    @Override public String display() { return value ? "on" : "off"; }

    @Override public boolean parse(String text) {
        switch (text.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "1", "enable", "enabled" -> set(true);
            case "false", "off", "no", "0", "disable", "disabled" -> set(false);
            case "toggle" -> toggle();
            default -> { return false; }
        }
        return true;
    }

    @Override public List<String> suggestions() { return List.of("on", "off", "toggle"); }

    @Override public BoolSetting visibleWhen(java.util.function.BooleanSupplier supplier) {
        super.visibleWhen(supplier);
        return this;
    }
}
