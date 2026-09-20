package me.wolfii.clientalarms.time;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public final class WorldKeys {
    private WorldKeys() {
    }

    public static String currentWorldKey(Minecraft minecraft) {
        IntegratedServer integrated = minecraft.getSingleplayerServer();
        if (integrated != null) {
            return "sp:" + integrated.getWorldData().getLevelName();
        }
        ServerData server = minecraft.getCurrentServer();
        if (server != null) {
            String address = server.ip == null || server.ip.isBlank() ? server.name : server.ip;
            return "mp:" + address;
        }
        if (minecraft.level != null) {
            return "level:" + minecraft.level.dimension().identifier();
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
