package me.wolfii.clienttimers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.wolfii.clienttimers.client.ClientTimersClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PresetStore {
    public static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("clienttimers").resolve("presets");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, CachedPreset> CACHE = new LinkedHashMap<>();
    private static boolean initialized;

    private PresetStore() {
    }

    public static synchronized void initialize(Config config) {
        if (!initialized) {
            try {
                Files.createDirectories(DIRECTORY);
            } catch (IOException exception) {
                ClientTimersClient.LOGGER.warn("Failed to create ringtone preset directory", exception);
            }
            writeReadmeIfMissing();
            migrate(config);
            writeDefaultIfMissing("Beep.json", defaultBeep());
            writeDefaultIfMissing("Bell.json", defaultBell());
            initialized = true;
        }
        reload();
    }

    public static synchronized void reload() {
        CACHE.clear();
        if (!Files.isDirectory(DIRECTORY)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIRECTORY, "*.json")) {
            for (Path path : stream) {
                load(path);
            }
        } catch (IOException exception) {
            ClientTimersClient.LOGGER.warn("Failed to list ringtone presets", exception);
        }
    }

    public static synchronized List<String> names() {
        refreshCache();
        List<String> names = new ArrayList<>(CACHE.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static synchronized SoundPreset get(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        refreshCache();
        CachedPreset cached = byName(name);
        return cached == null ? null : cached.preset.copy();
    }

    private static void migrate(Config config) {
        if (config.notes != null && !config.notes.isEmpty() && config.selectedPreset != null && !config.selectedPreset.isBlank()) {
            PresetFile file = new PresetFile();
            file.silenceTicks = config.silenceTicksBetweenRepeats;
            file.notes = config.notes;
            writeDefaultIfMissing(config.selectedPreset.trim() + ".json", file);
        }
        if (config.presets == null) {
            return;
        }
        for (SoundPreset preset : config.presets) {
            if (preset == null || preset.name == null || preset.name.isBlank()) {
                continue;
            }
            writeDefaultIfMissing(preset.name.trim() + ".json", toFile(preset));
        }
    }

    private static void refreshCache() {
        if (!Files.isDirectory(DIRECTORY)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIRECTORY, "*.json")) {
            Map<String, Path> found = new LinkedHashMap<>();
            for (Path path : stream) {
                String stem = stem(path);
                if (stem != null) {
                    found.put(stem, path);
                }
            }
            CACHE.keySet().removeIf(name -> !found.containsKey(name));
            for (Map.Entry<String, Path> entry : found.entrySet()) {
                Path path = entry.getValue();
                long mtime = mtime(path);
                CachedPreset cached = CACHE.get(entry.getKey());
                if (cached == null || cached.mtime != mtime) {
                    load(path);
                }
            }
        } catch (IOException exception) {
            ClientTimersClient.LOGGER.warn("Failed to refresh ringtone presets", exception);
        }
    }

    private static CachedPreset byName(String name) {
        CachedPreset cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        for (Map.Entry<String, CachedPreset> entry : CACHE.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static void load(Path path) {
        String stem = stem(path);
        if (stem == null) {
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            PresetFile parsed = GSON.fromJson(json, PresetFile.class);
            if (parsed == null) {
                parsed = new PresetFile();
            }
            if (parsed.notes == null) {
                parsed.notes = new ArrayList<>();
            }
            CACHE.put(stem, new CachedPreset(mtime(path), new SoundPreset(stem, parsed.silenceTicks, parsed.notes)));
        } catch (Exception exception) {
            ClientTimersClient.LOGGER.warn("Failed to read ringtone preset {}", path.getFileName(), exception);
        }
    }

    private static void writeDefaultIfMissing(String fileName, PresetFile preset) {
        Path path = DIRECTORY.resolve(fileName);
        if (Files.exists(path) || !Files.isDirectory(DIRECTORY)) {
            return;
        }
        try {
            Files.writeString(path, GSON.toJson(preset), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            ClientTimersClient.LOGGER.warn("Failed to write ringtone {}", fileName, exception);
        }
    }

    private static void writeReadmeIfMissing() {
        Path path = DIRECTORY.resolve("README.txt");
        if (Files.exists(path)) {
            return;
        }
        String readme = """
            Each JSON file in this folder is a ringtone. The filename (without .json) is the name shown in Client Timers.

            Example:
            {
              "silenceTicks": 40,
              "notes": [
                { "soundId": "minecraft:block.note_block.pling", "volume": 1.0, "pitch": 1.0, "tick": 0 },
                { "soundId": "minecraft:block.note_block.pling", "volume": 1.0, "pitch": 1.0, "tick": 4 }
              ]
            }

            tick is the offset in the repeating sequence. Invalid notes are skipped when playing.
            """;
        try {
            Files.writeString(path, readme, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            ClientTimersClient.LOGGER.warn("Failed to write ringtone preset README", exception);
        }
    }

    private static String stem(Path path) {
        String fileName = path.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
            return null;
        }
        String stem = fileName.substring(0, fileName.length() - 5);
        return stem.isBlank() ? null : stem;
    }

    private static long mtime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return -1L;
        }
    }

    private static PresetFile toFile(SoundPreset preset) {
        PresetFile file = new PresetFile();
        file.silenceTicks = preset.silenceTicks;
        file.notes = preset.notes;
        return file;
    }

    private static PresetFile defaultBeep() {
        PresetFile file = new PresetFile();
        file.silenceTicks = 40;
        file.notes = List.of(
            new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.0f, 0),
            new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.0f, 4),
            new AlarmNote("minecraft:block.note_block.pling", 1.0f, 1.0f, 8)
        );
        return file;
    }

    private static PresetFile defaultBell() {
        PresetFile file = new PresetFile();
        file.silenceTicks = 30;
        file.notes = List.of(
            new AlarmNote("minecraft:block.note_block.bell", 1.0f, 1.0f, 0)
        );
        return file;
    }

    private record CachedPreset(long mtime, SoundPreset preset) {
    }

    public static class PresetFile {
        public int silenceTicks = 40;
        public List<AlarmNote> notes = new ArrayList<>();
    }
}
