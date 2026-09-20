package me.wolfii.clienttimers.client;

import me.wolfii.clienttimers.ClientAlarms;
import me.wolfii.clienttimers.command.ClientAlarmCommands;
import me.wolfii.clienttimers.config.Config;
import me.wolfii.clienttimers.engine.AlarmEngine;
import me.wolfii.clienttimers.hud.OverlayHudElement;
import me.wolfii.clienttimers.hud.ScreenAlarmButtons;
import me.wolfii.clienttimers.notify.SoundPlayer;
import me.wolfii.clienttimers.persist.StateStore;
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
        Runtime.getRuntime().addShutdownHook(new Thread(AlarmEngine::persistOnShutdown, "clienttimers-shutdown"));
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            SoundPlayer.stopPreview(minecraft);
            AlarmEngine.persistOnShutdown();
        });
    }
}
