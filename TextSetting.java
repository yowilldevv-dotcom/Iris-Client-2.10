package dev.iris.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Free-text setting with a maximum length. Control characters are stripped. */
public class TextSetting extends Setting<String> {
    private final int maxLength;

    public TextSetting(String name, String description, String def, int maxLength) {
        super(name, description, def);
        this.maxLength = maxLength;
        this.value = sanitize(def);
    }

    public int maxLength() { return maxLength; }

    @Override protected String sanitize(String candidate) {
        if (candidate == null) return "";
        String s = candidate.replaceAll("\\p{Cntrl}", "");
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement element) {
        if (!element.isJsonPrimitive()) throw new IllegalArgumentException("expected string");
        set(element.getAsString());
    }

    @Override public String display() { return value.isEmpty() ? "(empty)" : value; }

    @Override public boolean parse(String text) {
        set(text);
        return true;
    }

    @Override public TextSetting visibleWhen(java.util.function.BooleanSupplier supplier) {
        super.visibleWhen(supplier);
        return this;
    }
}
