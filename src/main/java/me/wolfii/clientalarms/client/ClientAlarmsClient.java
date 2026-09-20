package me.wolfii.clientalarms.client;

import me.wolfii.clientalarms.ClientAlarms;
import me.wolfii.clientalarms.command.ClientAlarmCommands;
import me.wolfii.clientalarms.config.Config;
import me.wolfii.clientalarms.engine.AlarmEngine;
import me.wolfii.clientalarms.hud.OverlayHudElement;
import me.wolfii.clientalarms.hud.ScreenAlarmButtons;
import me.wolfii.clientalarms.notify.SoundPlayer;
import me.wolfii.clientalarms.persist.StateStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class ClientAlarmsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.get().ensureDefaults();
        StateStore.loadBlocking();
        ClientAlarmCommands.register();
        HudElementRegistry.addLast(ClientAlarms.id("overlay"), new OverlayHudElement());
        ScreenAlarmButtons.register();
        ClientTickEvents.END_CLIENT_TICK.register(AlarmEngine::tick);
        Runtime.getRuntime().addShutdownHook(new Thread(AlarmEngine::persistOnShutdown, "clientalarms-shutdown"));
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            SoundPlayer.stopPreview(minecraft);
            AlarmEngine.persistOnShutdown();
        });
    }
}
