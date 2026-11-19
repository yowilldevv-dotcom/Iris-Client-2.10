package dev.iris.client.setting;

import com.google.gson.JsonElement;
import dev.iris.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base class for every configurable value in Iris (module options, HUD options, client preferences).
 * A setting knows how to clamp/validate itself, serialize to JSON and parse from command text,
 * so the GUI, commands and config manager all share one implementation.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    protected T value;
    private BooleanSupplier visibility = () -> true;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String name() { return name; }
    public String description() { return description; }

    /** Command-friendly identifier: lowercase, spaces replaced with dashes. */
    public String key() { return name.toLowerCase().replace(' ', '-'); }

    public T get() { return value; }
    public T defaultValue() { return defaultValue; }
    public boolean isDefault() { return Objects.equals(value, defaultValue); }

    /** Sets the value after sanitizing it. Returns true when the stored value actually changed. */
    public final boolean set(T newValue) {
        T clean = sanitize(newValue);
        if (Objects.equals(clean, value)) return false;
        value = clean;
        for (Consumer<T> l : new ArrayList<>(listeners)) {
            try {
                l.accept(clean);
            } catch (RuntimeException e) {
                dev.iris.client.core.Iris.LOGGER.warn("Listener of setting '{}' failed", name, e);
            }
        }
        ConfigManager.markDirty();
        return true;
    }

    public void reset() { set(defaultValue); }

    /** Hides this setting in the GUI while the supplier returns false. */
    public Setting<T> visibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return this;
    }

    public boolean isVisible() {
        try {
            return visibility.getAsBoolean();
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** Registers a change listener. Also fires while loading a configuration. */
    public Setting<T> onChange(Consumer<T> listener) {
        listeners.add(listener);
        return this;
    }

    /** Clamp/normalize a candidate value. Must be idempotent. */
    protected abstract T sanitize(T candidate);

    public abstract JsonElement toJson();

    /** Reads a value from JSON. Throws IllegalArgumentException/ClassCastException/IllegalStateException on bad input. */
    public abstract void fromJson(JsonElement element);

    /** Short human-readable value, used by the GUI and the command system. */
    public abstract String display();

    /** Parses command text. Returns false if the text is not valid for this setting. */
    public abstract boolean parse(String text);

    /** Tab-completion candidates for this setting's value. */
    public List<String> suggestions() { return List.of(); }
}
