package dev.iris.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.iris.client.core.Iris;
import dev.iris.client.hud.HudElement;
import dev.iris.client.module.Module;
import dev.iris.client.setting.KeyNames;
import dev.iris.client.setting.SettingGroup;
import dev.iris.client.util.Names;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Multi-profile configuration manager. A configuration ("profile") stores module states, module settings, keybinds,
 * HUD layout, the selected GUI theme and client/notification preferences. Files live in
 * {@code config/irisclient/profiles/<name>.json}. Every read is defensive: a corrupt or invalid file is quarantined
 * and defaults are used instead of crashing the client.
 */
public final class ConfigManager {
    public static final int FORMAT = 1;
    private static final long AUTOSAVE_INTERVAL_MS = 5_000;

    private static volatile boolean dirty;
    private static volatile boolean loading;

    private final Path root = FabricLoader.getInstance().getConfigDir().resolve("irisclient");
    private final Path profilesDir = root.resolve("profiles");
    private final Path themesDir = root.resolve("themes");
    private final Path stateFile = root.resolve("state.json");

    private String active = "default";
    private long lastSave = System.currentTimeMillis();

    public static void markDirty() {
        if (!loading) dirty = true;
    }

    public static boolean isLoading() { return loading; }

    public Path root() { return root; }
    public Path themesDir() { return themesDir; }
    public Path profilesDir() { return profilesDir; }
    public String activeName() { return active; }

    // ---- startup ----

    /** Creates folders and loads the active profile. Never throws. */
    public void init() {
        try {
            Files.createDirectories(profilesDir);
            Files.createDirectories(themesDir);
        } catch (IOException e) {
            Iris.LOGGER.error("Could not create config folders", e);
        }
        Iris.themes().loadCustoms();
        readState();
        Path file = profileFile(active);
        if (!Files.exists(file)) {
            applyDefaults();
            safeSave(active);
            return;
        }
        try {
            List<String> warnings = applyFile(file);
            for (String w : warnings) Iris.LOGGER.warn("Config warning: {}", w);
        } catch (IOException e) {
            Path moved = JsonFiles.quarantine(file);
            Iris.LOGGER.error("Config '{}' was unreadable ({}); moved to {}", active, e.getMessage(), moved);
            applyDefaults();
            safeSave(active);
            Iris.notifications().error("Config recovered", "'" + active + "' was corrupt; using defaults.");
        }
    }

    private void readState() {
        try {
            if (Files.exists(stateFile)) {
                JsonObject o = JsonFiles.read(stateFile);
                JsonElement a = o.get("active");
                if (a != null && a.isJsonPrimitive() && Names.isValid(a.getAsString())) active = a.getAsString();
            }
        } catch (IOException | RuntimeException e) {
            Iris.LOGGER.warn("Could not read state.json: {}", e.getMessage());
            JsonFiles.quarantine(stateFile);
        }
    }

    private void writeState() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("active", active);
            JsonFiles.write(stateFile, o);
        } catch (IOException e) {
            Iris.LOGGER.warn("Could not write state.json: {}", e.getMessage());
        }
    }

    // ---- snapshot / apply ----

    public JsonObject snapshot() {
        JsonObject o = new JsonObject();
        o.addProperty("format", FORMAT);
        o.addProperty("client", "Iris");

        JsonObject modules = new JsonObject();
        for (Module m : Iris.modules().all()) {
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", m.isEnabled());
            mo.addProperty("key", KeyNames.name(m.key()));
            mo.addProperty("listed", m.isShownInList());
            mo.add("settings", m.saveSettings());
            modules.add(m.id(), mo);
        }
        o.add("modules", modules);

        JsonObject hud = new JsonObject();
        for (HudElement e : Iris.hud().all()) hud.add(e.id(), e.save());
        o.add("hud", hud);

        o.addProperty("theme", Iris.themes().current().name());

        JsonObject groups = new JsonObject();
        for (Map.Entry<String, SettingGroup> g : Iris.settingGroups().entrySet()) {
            groups.add(g.getKey(), g.getValue().saveSettings());
        }
        o.add("groups", groups);
        return o;
    }

    /** Resets everything to factory defaults without touching files. */
    public void applyDefaults() {
        loading = true;
        try {
            for (Module m : Iris.modules().all()) {
                m.setEnabled(false, false);
                m.resetSettings();
                m.setKey(KeyNames.NONE);
                m.setShownInList(true);
            }
            for (HudElement e : Iris.hud().all()) e.resetAll();
            for (SettingGroup g : Iris.settingGroups().values()) g.resetSettings();
            Iris.themes().setCurrentOrDefault("Galaxy");
        } finally {
            loading = false;
        }
        Iris.onSettingsReloaded();
    }

    /** Applies a snapshot leniently. Returns warnings for entries that were invalid and skipped. */
    public List<String> apply(JsonObject o) {
        List<String> warnings = new ArrayList<>();
        loading = true;
        try {
            int fmt = o.has("format") && o.get("format").isJsonPrimitive() ? o.get("format").getAsInt() : 1;
            if (fmt > FORMAT) warnings.add("config was written by a newer Iris (format " + fmt + "); loading what is understood");

            // Start from a clean slate so profiles never leak into each other.
            for (Module m : Iris.modules().all()) {
                m.setEnabled(false, false);
                m.resetSettings();
                m.setKey(KeyNames.NONE);
                m.setShownInList(true);
            }
            for (HudElement e : Iris.hud().all()) e.resetAll();
            for (SettingGroup g : Iris.settingGroups().values()) g.resetSettings();

            applyModules(o, warnings);
            applyHud(o, warnings);
            applyGroups(o, warnings);
            String theme = str(o, "theme");
            Iris.themes().setCurrentOrDefault(theme != null ? theme : "Galaxy");
            if (theme != null && Iris.themes().get(theme) == null) warnings.add("theme '" + theme + "' not found; using Galaxy");
        } catch (RuntimeException e) {
            warnings.add("unexpected error while applying config: " + e);
            Iris.LOGGER.error("Config apply failed", e);
        } finally {
            loading = false;
        }
        Iris.onSettingsReloaded();
        return warnings;
    }

    private void applyModules(JsonObject o, List<String> warnings) {
        JsonObject modules = obj(o, "modules");
        if (modules == null) return;
        for (Map.Entry<String, JsonElement> entry : modules.entrySet()) {
            Module m = Iris.modules().get(entry.getKey());
            if (m == null || !entry.getValue().isJsonObject()) {
                if (m == null) warnings.add("unknown module '" + entry.getKey() + "' ignored");
                continue;
            }
            JsonObject mo = entry.getValue().getAsJsonObject();
            JsonObject settings = obj(mo, "settings");
            if (settings != null) warnings.addAll(m.loadSettings(settings));
            String key = str(mo, "key");
            if (key != null) {
                int code = KeyNames.parse(key);
                if (code == Integer.MIN_VALUE) warnings.add(m.name() + ": unknown key '" + key + "'");
                else m.setKey(code);
            }
            JsonElement listed = mo.get("listed");
            if (listed != null && listed.isJsonPrimitive() && listed.getAsJsonPrimitive().isBoolean()) {
                m.setShownInList(listed.getAsBoolean());
            }
            JsonElement en = mo.get("enabled");
            if (en != null && en.isJsonPrimitive() && en.getAsJsonPrimitive().isBoolean() && en.getAsBoolean()) {
                m.setEnabled(true, false);
            }
        }
    }

    private void applyHud(JsonObject o, List<String> warnings) {
        JsonObject hud = obj(o, "hud");
        if (hud == null) return;
        for (Map.Entry<String, JsonElement> entry : hud.entrySet()) {
            HudElement e = Iris.hud().get(entry.getKey());
            if (e == null || !entry.getValue().isJsonObject()) {
                if (e == null) warnings.add("unknown HUD element '" + entry.getKey() + "' ignored");
                continue;
            }
            warnings.addAll(e.load(entry.getValue().getAsJsonObject()));
        }
    }

    private void applyGroups(JsonObject o, List<String> warnings) {
        JsonObject groups = obj(o, "groups");
        if (groups == null) return;
        for (Map.Entry<String, SettingGroup> g : Iris.settingGroups().entrySet()) {
            JsonObject go = obj(groups, g.getKey());
            if (go != null) warnings.addAll(g.getValue().loadSettings(go));
        }
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    // ---- saving ----

    private Path profileFile(String name) {
        return profilesDir.resolve(Names.fileStem(name) + ".json");
    }

    public void saveActive() {
        safeSave(active);
        dirty = false;
        lastSave = System.currentTimeMillis();
    }

    private void safeSave(String name) {
        try {
            JsonFiles.write(profileFile(name), snapshot());
            writeState();
        } catch (IOException | RuntimeException e) {
            Iris.LOGGER.error("Could not save config '{}'", name, e);
            Iris.notifications().error("Save failed", e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /** Called every client tick: debounced autosave. */
    public void tick() {
        if (dirty && Iris.client().autosave.on() && System.currentTimeMillis() - lastSave >= AUTOSAVE_INTERVAL_MS) {
            saveActive();
        }
    }

    // ---- profile management ----

    public List<String> list() {
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDir, "*.json")) {
            for (Path p : stream) {
                String n = p.getFileName().toString();
                out.add(n.substring(0, n.length() - 5).replace('_', ' '));
            }
        } catch (IOException e) {
            Iris.LOGGER.warn("Could not list configs: {}", e.getMessage());
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public boolean exists(String name) {
        return name != null && Names.isValid(name) && Files.exists(profileFile(name));
    }

    private String resolveExisting(String name) {
        for (String n : list()) if (n.equalsIgnoreCase(name.trim())) return n;
        return null;
    }

    private void requireNew(String name) {
        if (!Names.isValid(name)) throw new IllegalArgumentException("Invalid name. Use " + Names.requirements() + ".");
        if (resolveExisting(name) != null) throw new IllegalArgumentException("A config named '" + name + "' already exists.");
    }

    /** Creates a new profile containing factory defaults (does not switch to it). */
    public void create(String name) {
        requireNew(name);
        // Loading applies a clean slate first, so a profile with empty sections is exactly "factory defaults".
        JsonObject fresh = new JsonObject();
        fresh.addProperty("format", FORMAT);
        fresh.addProperty("client", "Iris");
        fresh.add("modules", new JsonObject());
        fresh.add("hud", new JsonObject());
        fresh.add("groups", new JsonObject());
        fresh.addProperty("theme", "Galaxy");
        try {
            JsonFiles.write(profileFile(name), fresh);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create config: " + e.getMessage(), e);
        }
    }

    /** Saves the active profile and switches to {@code name}. Throws IOException when the target is unreadable. */
    public List<String> load(String name) throws IOException {
        String real = resolveExisting(name);
        if (real == null) throw new IllegalArgumentException("No config named '" + name + "'.");
        saveActive();
        Path file = profileFile(real);
        List<String> warnings;
        try {
            warnings = applyFile(file);
        } catch (IOException e) {
            // Restore the previous profile's state instead of leaving a half-applied config.
            try {
                applyFile(profileFile(active));
            } catch (IOException ignored) {
                applyDefaults();
            }
            throw new IOException("'" + real + "' is corrupt or invalid (" + e.getMessage() + "); kept '" + active + "'.", e);
        }
        active = real;
        writeState();
        dirty = false;
        Iris.notifications().configEvent("Config loaded", real);
        return warnings;
    }

    private List<String> applyFile(Path file) throws IOException {
        JsonObject o = JsonFiles.read(file);
        if (!o.has("modules") && !o.has("hud") && !o.has("groups")) throw new IOException("not an Iris configuration");
        return apply(o);
    }

    public void duplicate(String source, String target) {
        String real = resolveExisting(source);
        if (real == null) throw new IllegalArgumentException("No config named '" + source + "'.");
        requireNew(target);
        if (real.equals(active)) saveActive();
        try {
            Files.copy(profileFile(real), profileFile(target));
        } catch (IOException e) {
            throw new IllegalStateException("Could not duplicate config: " + e.getMessage(), e);
        }
    }

    public void rename(String from, String to) {
        String real = resolveExisting(from);
        if (real == null) throw new IllegalArgumentException("No config named '" + from + "'.");
        if (!real.equalsIgnoreCase(to)) requireNew(to);
        else if (!Names.isValid(to)) throw new IllegalArgumentException("Invalid name. Use " + Names.requirements() + ".");
        boolean wasActive = real.equals(active);
        if (wasActive) saveActive();
        try {
            Path target = profileFile(to);
            Path source = profileFile(real);
            if (!source.equals(target)) Files.move(source, target);
        } catch (IOException e) {
            throw new IllegalStateException("Could not rename config: " + e.getMessage(), e);
        }
        if (wasActive) {
            active = to.trim();
            writeState();
        }
    }

    public void delete(String name) {
        String real = resolveExisting(name);
        if (real == null) throw new IllegalArgumentException("No config named '" + name + "'.");
        if (real.equals(active)) throw new IllegalArgumentException("Cannot delete the active config. Load another one first.");
        try {
            Files.deleteIfExists(profileFile(real));
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete config: " + e.getMessage(), e);
        }
    }

    public void exportTo(String name, Path destination) throws IOException {
        String real = resolveExisting(name);
        if (real == null) throw new IllegalArgumentException("No config named '" + name + "'.");
        if (real.equals(active)) saveActive();
        Files.createDirectories(destination.toAbsolutePath().getParent());
        Files.copy(profileFile(real), destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /** Imports an external file as a new profile after validating that it is a readable Iris configuration. */
    public String importFrom(Path source, String requestedName) throws IOException {
        if (!Files.isRegularFile(source)) throw new IOException("file not found: " + source);
        JsonObject o = JsonFiles.read(source);
        if (!o.has("modules") && !o.has("hud") && !o.has("groups")) throw new IOException("not an Iris configuration");
        String base = requestedName;
        if (base == null || base.isBlank()) {
            String fn = source.getFileName().toString();
            base = fn.toLowerCase(Locale.ROOT).endsWith(".json") ? fn.substring(0, fn.length() - 5) : fn;
            base = base.replace('_', ' ');
        }
        if (!Names.isValid(base)) throw new IOException("invalid config name '" + base + "'. Use " + Names.requirements() + ".");
        String name = base;
        int n = 2;
        while (resolveExisting(name) != null) name = base + " " + n++;
        JsonFiles.write(profileFile(name), o);
        return name;
    }

    public Path fileOf(String name) { return profileFile(name); }
}
