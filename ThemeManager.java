package dev.iris.client.theme;

import com.google.gson.JsonObject;
import dev.iris.client.config.ConfigManager;
import dev.iris.client.config.JsonFiles;
import dev.iris.client.core.Iris;
import dev.iris.client.util.Names;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Owns the built-in themes, the user's custom themes (stored in {@code config/irisclient/themes}) and the active
 * theme. GUI code calls {@link #current()} every frame, so changes apply instantly everywhere.
 */
public final class ThemeManager {
    private static final String DEFAULT = "Galaxy";

    private final List<Theme> builtins = new ArrayList<>();
    private final List<Theme> customs = new ArrayList<>();
    private Theme current;

    public ThemeManager() {
        builtin("Galaxy",     0xB005030F, 0xF0120B2E, 0xF01D1442, 0xFF1A0F40, 0xFF8B5CF6, 0xFF22D3EE, 0xFFF3F0FF, 0xFF9C94C8, 0xFF3B2A7A);
        builtin("Nebula",     0xB0120614, 0xF01B0B26, 0xF02A1238, 0xFF2A1040, 0xFFEC4899, 0xFF8B5CF6, 0xFFFDF2F8, 0xFFC4A1D6, 0xFF5B2A6E);
        builtin("Midnight",   0xB0020510, 0xF00B1220, 0xF0141D33, 0xFF111A30, 0xFF3B82F6, 0xFF60A5FA, 0xFFE8EEFF, 0xFF8FA0C4, 0xFF22304F);
        builtin("Aurora",     0xB0021210, 0xF008201F, 0xF0102E2C, 0xFF0C2A28, 0xFF34D399, 0xFF818CF8, 0xFFEAFFF7, 0xFF8FC9B6, 0xFF1B5C52);
        builtin("Cyberpunk",  0xB00A0714, 0xF00D0B1A, 0xF0181430, 0xFF14102A, 0xFFFCEE0A, 0xFFFF2A6D, 0xFFFFFBD6, 0xFFB7A98A, 0xFF4B2A5E);
        builtin("Ocean",      0xB0021018, 0xF006202E, 0xF00D3044, 0xFF0A2A3C, 0xFF06B6D4, 0xFF3B82F6, 0xFFE6FAFF, 0xFF8CBFCF, 0xFF16506A);
        builtin("Crimson",    0xB0140205, 0xF01F0A0E, 0xF02D1218, 0xFF2A0F16, 0xFFEF4444, 0xFFF97316, 0xFFFFEDED, 0xFFC99A9A, 0xFF6A1F2A);
        builtin("Emerald",    0xB0021008, 0xF008201A, 0xF00F2F26, 0xFF0B2B22, 0xFF10B981, 0xFF84CC16, 0xFFEDFFF6, 0xFF93C9B1, 0xFF17614A);
        builtin("Classic",    0xA0000000, 0xF0303030, 0xF0404040, 0xFF2A2A2A, 0xFF5DAA3C, 0xFFC6C6C6, 0xFFFFFFFF, 0xFFA0A0A0, 0xFF555555);
        builtin("Pitch Black",0xC0000000, 0xFA000000, 0xFA121212, 0xFF080808, 0xFFFFFFFF, 0xFF9CA3AF, 0xFFFFFFFF, 0xFF8A8A8A, 0xFF2A2A2A);
        builtin("Sunset",     0xB01A0A0A, 0xF023101A, 0xF0341A26, 0xFF2E1520, 0xFFFB923C, 0xFFF43F5E, 0xFFFFF1E6, 0xFFD0A58E, 0xFF6B2E35);
        builtin("Daylight",   0x90FFFFFF, 0xF2F1F5F9, 0xF2E2E8F0, 0xFFE2E8F0, 0xFF2563EB, 0xFF0891B2, 0xFF0F172A, 0xFF475569, 0xFFCBD5E1);
        current = builtins.get(0);
    }

    private void builtin(String name, int bg, int panel, int alt, int header, int a1, int a2, int text, int dim, int outline) {
        builtins.add(new Theme(name, true, bg, panel, alt, header, a1, a2, text, dim, outline,
                0xFF4ADE80, 0xFFFBBF24, 0xFFF87171));
    }

    // ---- queries ----

    public Theme current() { return current; }

    public List<Theme> all() {
        List<Theme> out = new ArrayList<>(builtins);
        out.addAll(customs);
        return out;
    }

    public List<Theme> customs() { return List.copyOf(customs); }

    public Theme get(String name) {
        if (name == null) return null;
        for (Theme t : all()) if (t.name().equalsIgnoreCase(name.trim())) return t;
        return null;
    }

    // ---- selection ----

    public boolean setCurrent(String name) {
        Theme t = get(name);
        if (t == null) return false;
        current = t;
        ConfigManager.markDirty();
        return true;
    }

    /** Used when loading configurations: falls back to the default theme if the saved one is gone. */
    public void setCurrentOrDefault(String name) {
        Theme t = get(name);
        current = t != null ? t : builtins.get(0);
    }

    // ---- custom theme management ----

    private Path dir() { return Iris.config().themesDir(); }

    private Path fileFor(Theme t) { return dir().resolve(Names.fileStem(t.name()) + ".json"); }

    public void loadCustoms() {
        customs.clear();
        Path d = dir();
        if (!Files.isDirectory(d)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(d, "*.json")) {
            for (Path p : stream) {
                try {
                    Theme t = Theme.fromJson(JsonFiles.read(p), builtins.get(0));
                    if (!Names.isValid(t.name()) || get(t.name()) != null) {
                        Iris.LOGGER.warn("Skipping theme file {}: invalid or duplicate name", p.getFileName());
                        continue;
                    }
                    customs.add(t);
                } catch (IOException | RuntimeException e) {
                    Iris.LOGGER.warn("Skipping unreadable theme file {}: {}", p.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            Iris.LOGGER.warn("Could not list themes: {}", e.getMessage());
        }
        customs.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
    }

    private void checkNewName(String name) {
        if (!Names.isValid(name)) throw new IllegalArgumentException("Invalid name. Use " + Names.requirements() + ".");
        if (get(name) != null) throw new IllegalArgumentException("A theme named '" + name + "' already exists.");
    }

    /** Creates an editable copy of {@code base} and saves it. */
    public Theme create(String name, Theme base) {
        checkNewName(name);
        Theme t = base.copyAs(name.trim());
        customs.add(t);
        customs.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        save(t);
        return t;
    }

    public void save(Theme t) {
        if (t.isBuiltin()) throw new IllegalArgumentException("Built-in themes cannot be saved.");
        try {
            JsonFiles.write(fileFor(t), t.toJson());
        } catch (IOException e) {
            throw new IllegalStateException("Could not save theme: " + e.getMessage(), e);
        }
    }

    public void rename(String oldName, String newName) {
        Theme t = get(oldName);
        if (t == null) throw new IllegalArgumentException("No theme named '" + oldName + "'.");
        if (t.isBuiltin()) throw new IllegalArgumentException("Built-in themes cannot be renamed.");
        if (!oldName.equalsIgnoreCase(newName)) checkNewName(newName);
        else if (!Names.isValid(newName)) throw new IllegalArgumentException("Invalid name. Use " + Names.requirements() + ".");
        try {
            Files.deleteIfExists(fileFor(t));
        } catch (IOException e) {
            throw new IllegalStateException("Could not rename theme file: " + e.getMessage(), e);
        }
        t.rename(newName.trim());
        save(t);
        customs.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        ConfigManager.markDirty();
    }

    public void delete(String name) {
        Theme t = get(name);
        if (t == null) throw new IllegalArgumentException("No theme named '" + name + "'.");
        if (t.isBuiltin()) throw new IllegalArgumentException("Built-in themes cannot be deleted.");
        try {
            Files.deleteIfExists(fileFor(t));
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete theme file: " + e.getMessage(), e);
        }
        customs.remove(t);
        if (current == t) {
            current = builtins.get(0);
            ConfigManager.markDirty();
        }
    }

    // ---- import / export ----

    public String exportString(String name) {
        Theme t = get(name);
        if (t == null) throw new IllegalArgumentException("No theme named '" + name + "'.");
        return JsonFiles.toString(t.toJson());
    }

    public void exportTo(String name, Path file) throws IOException {
        Theme t = get(name);
        if (t == null) throw new IllegalArgumentException("No theme named '" + name + "'.");
        JsonFiles.write(file, t.toJson());
    }

    /** Imports a theme from JSON text. A name clash is resolved by appending a number. */
    public Theme importString(String json) throws IOException {
        JsonObject o = JsonFiles.parseObject(json);
        Theme parsed;
        try {
            parsed = Theme.fromJson(o, builtins.get(0));
        } catch (IllegalArgumentException e) {
            throw new IOException("invalid theme: " + e.getMessage(), e);
        }
        String base = parsed.name().trim();
        if (!Names.isValid(base)) throw new IOException("invalid theme name '" + base + "'");
        String name = base;
        int n = 2;
        while (get(name) != null) name = base + " " + n++;
        if (!Names.isValid(name)) name = "Imported " + System.currentTimeMillis() % 10000;
        Theme t = parsed.copyAs(name);
        customs.add(t);
        customs.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        save(t);
        return t;
    }

    public Theme importFrom(Path file) throws IOException {
        if (Files.size(file) > JsonFiles.MAX_BYTES) throw new IOException("file too large");
        return importString(Files.readString(file));
    }

    public List<String> names() {
        List<String> out = new ArrayList<>();
        for (Theme t : all()) out.add(t.name());
        return out;
    }

    public static String normalize(String s) { return s.toLowerCase(Locale.ROOT).trim(); }
}
