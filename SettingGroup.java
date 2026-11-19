package dev.iris.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A named collection of settings. Modules, HUD elements and client preference panels all extend this, so the
 * settings panel, the {@code set}/{@code get} commands and the config manager treat them uniformly.
 */
public class SettingGroup {
    private final String name;
    private final String description;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final Map<String, Setting<?>> byKey = new HashMap<>();

    public SettingGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String name() { return name; }
    public String description() { return description; }

    /** Command-friendly identifier. */
    public String id() { return name.toLowerCase().replace(' ', '-'); }

    protected <S extends Setting<?>> S add(S setting) {
        if (byKey.putIfAbsent(setting.key(), setting) != null) {
            throw new IllegalStateException("Duplicate setting '" + setting.key() + "' in " + name);
        }
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }

    public Optional<Setting<?>> find(String key) {
        return Optional.ofNullable(byKey.get(key.toLowerCase().replace(' ', '-')));
    }

    public void resetSettings() { for (Setting<?> s : settings) s.reset(); }

    public JsonObject saveSettings() {
        JsonObject o = new JsonObject();
        for (Setting<?> s : settings) o.add(s.key(), s.toJson());
        return o;
    }

    /** Loads values leniently: a bad entry is skipped and reported, never thrown. */
    public List<String> loadSettings(JsonObject o) {
        List<String> warnings = new ArrayList<>();
        for (Setting<?> s : settings) {
            JsonElement e = o.get(s.key());
            if (e == null || e.isJsonNull()) continue;
            try {
                s.fromJson(e);
            } catch (RuntimeException ex) {
                warnings.add(name + "." + s.key() + ": " + ex.getMessage());
            }
        }
        return warnings;
    }
}
