package dev.iris.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** Dropdown setting: one value out of a fixed list of options. */
public class ModeSetting extends Setting<String> {
    private final List<String> options;

    public ModeSetting(String name, String description, String def, String... options) {
        super(name, description, def);
        if (options.length == 0) throw new IllegalArgumentException("no options for " + name);
        this.options = List.of(options);
        if (!this.options.contains(def)) throw new IllegalArgumentException("default not in options for " + name);
    }

    public List<String> options() { return options; }
    public boolean is(String option) { return value.equalsIgnoreCase(option); }
    public int index() { return Math.max(0, options.indexOf(value)); }

    public void cycle(int direction) {
        int n = options.size();
        set(options.get(((index() + direction) % n + n) % n));
    }

    @Override protected String sanitize(String candidate) {
        if (candidate != null) {
            for (String o : options) if (o.equalsIgnoreCase(candidate)) return o;
        }
        return defaultValue();
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement element) {
        if (!element.isJsonPrimitive()) throw new IllegalArgumentException("expected string");
        String s = element.getAsString();
        for (String o : options) {
            if (o.equalsIgnoreCase(s)) { set(o); return; }
        }
        throw new IllegalArgumentException("unknown option '" + s + "'");
    }

    @Override public String display() { return value; }

    @Override public boolean parse(String text) {
        for (String o : options) {
            if (o.equalsIgnoreCase(text.trim()) || o.replace(' ', '-').equalsIgnoreCase(text.trim())) {
                set(o);
                return true;
            }
        }
        return false;
    }

    @Override public List<String> suggestions() { return options; }

    @Override public ModeSetting visibleWhen(java.util.function.BooleanSupplier supplier) {
        super.visibleWhen(supplier);
        return this;
    }
}
