package me.wolfii.clienttimers.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public final class WorldKeys {
    private WorldKeys() {
    }

    public static String currentWorldKey(Minecraft minecraft) {
        IntegratedServer integrated = minecraft.getSingleplayerServer();
        if (integrated != null) {
            return "sp:" + Long.toUnsignedString(integrated.overworld().getSeed());
        }
        ServerData server = minecraft.getCurrentServer();
        if (server != null) {
            String address = server.ip.isBlank() ? server.name : server.ip;
            return "mp:" + address;
        }
        return "";
    }

    public static boolean inWorld(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && minecraft.getConnection() != null;
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
