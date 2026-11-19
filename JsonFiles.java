package dev.iris.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Safe JSON file IO: atomic writes, size limits and validation so corrupt files can never crash the client. */
public final class JsonFiles {
    /** Anything larger than this is rejected as not being a config file. */
    public static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private JsonFiles() {}

    public static String toString(JsonElement e) { return GSON.toJson(e); }

    /** Parses a JSON object from text. Throws IOException with a readable message on any problem. */
    public static JsonObject parseObject(String text) throws IOException {
        try {
            JsonElement e = com.google.gson.JsonParser.parseString(text);
            if (e == null || !e.isJsonObject()) throw new IOException("not a JSON object");
            return e.getAsJsonObject();
        } catch (JsonParseException ex) {
            throw new IOException("invalid JSON: " + ex.getMessage(), ex);
        }
    }

    public static JsonObject read(Path file) throws IOException {
        long size = Files.size(file);
        if (size > MAX_BYTES) throw new IOException("file too large (" + size + " bytes)");
        return parseObject(Files.readString(file, StandardCharsets.UTF_8));
    }

    /** Writes via a temp file and an atomic rename so a crash mid-write cannot leave a half-written config. */
    public static void write(Path file, JsonElement content) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(content), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Moves a broken file aside (keeps it for inspection) so it is not loaded again. Returns the new path or null. */
    public static Path quarantine(Path file) {
        try {
            Path target = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            return null;
        }
    }
}
