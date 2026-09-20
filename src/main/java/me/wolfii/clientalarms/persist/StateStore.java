package me.wolfii.clientalarms.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.wolfii.clientalarms.ClientAlarms;
import me.wolfii.clientalarms.engine.AlarmEngine;
import me.wolfii.clientalarms.engine.Trackable;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public final class StateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "clientalarms-io");
        thread.setDaemon(true);
        return thread;
    });
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("clientalarms-state.json");

    private StateStore() {
    }

    public static void load() {
        IO.execute(() -> {
            Snapshot snapshot = readSnapshot();
            net.minecraft.client.Minecraft.getInstance().execute(() -> AlarmEngine.replaceAll(snapshot.entries));
        });
    }

    public static void saveAsync(List<Trackable> entries) {
        Snapshot snapshot = new Snapshot();
        snapshot.entries = copy(entries);
        try {
            IO.execute(() -> writeSnapshot(snapshot));
        } catch (RejectedExecutionException ignored) {
        }
    }

    public static void saveBlocking(List<Trackable> entries) {
        Snapshot snapshot = new Snapshot();
        snapshot.entries = copy(entries);
        writeSnapshot(snapshot);
    }

    public static void shutdown() {
        IO.shutdown();
    }

    private static Snapshot readSnapshot() {
        if (!Files.exists(PATH)) {
            return new Snapshot();
        }
        try {
            String json = Files.readString(PATH, StandardCharsets.UTF_8);
            Snapshot snapshot = GSON.fromJson(json, Snapshot.class);
            return snapshot == null ? new Snapshot() : snapshot;
        } catch (Exception exception) {
            ClientAlarms.LOGGER.warn("Failed to read client alarm state", exception);
            return new Snapshot();
        }
    }

    private static void writeSnapshot(Snapshot snapshot) {
        try {
            Files.createDirectories(PATH.getParent());
            Path temp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(snapshot), StandardCharsets.UTF_8);
            Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            try {
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(snapshot), StandardCharsets.UTF_8);
            } catch (IOException nested) {
                ClientAlarms.LOGGER.warn("Failed to write client alarm state", nested);
            }
        }
    }

    private static List<Trackable> copy(List<Trackable> entries) {
        List<Trackable> copy = new ArrayList<>();
        Gson gson = GSON;
        for (Trackable entry : entries) {
            copy.add(gson.fromJson(gson.toJson(entry), Trackable.class));
        }
        return copy;
    }

    public static class Snapshot {
        public List<Trackable> entries = new ArrayList<>();
    }
}
