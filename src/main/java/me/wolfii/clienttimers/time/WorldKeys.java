package me.wolfii.clienttimers.time;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public final class WorldKeys {
    private WorldKeys() {
    }

    public static String currentWorldKey(Minecraft minecraft) {
        IntegratedServer integrated = minecraft.getSingleplayerServer();
        if (integrated != null) {
            if (integrated.overworld() != null) {
                return "sp:" + Long.toUnsignedString(integrated.overworld().getSeed());
            }
            return "sp:" + Long.toUnsignedString(integrated.getWorldGenSettings().options().seed());
        }
        ServerData server = minecraft.getCurrentServer();
        if (server != null) {
            String address = server.ip == null || server.ip.isBlank() ? server.name : server.ip;
            return "mp:" + address;
        }
        return "";
    }

    public static boolean inWorld(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && minecraft.getConnection() != null;
    }

    public static boolean gameRunning(Minecraft minecraft) {
        if (minecraft.getOverlay() != null) {
            return false;
        }
        Screen screen = minecraft.screen;
        if (screen instanceof LevelLoadingScreen
            || screen instanceof ProgressScreen
            || screen instanceof ConnectScreen
            || screen instanceof GenericMessageScreen
            || screen instanceof GenericWaitingScreen
            || screen instanceof DisconnectedScreen) {
            return false;
        }
        return true;
    }

    public static long currentWorldTime(Minecraft minecraft) {
        if (minecraft.level == null) {
            return -1L;
        }
        return minecraft.level.getOverworldClockTime();
    }

    public static long currentWorldDay(Minecraft minecraft) {
        long time = currentWorldTime(minecraft);
        if (time < 0) {
            return -1L;
        }
        return time / 24000L;
    }
}
