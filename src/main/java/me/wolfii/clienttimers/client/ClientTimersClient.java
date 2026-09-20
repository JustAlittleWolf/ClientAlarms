package me.wolfii.clienttimers.client;

import me.wolfii.clienttimers.command.ClientAlarmCommands;
import me.wolfii.clienttimers.config.Config;
import me.wolfii.clienttimers.persist.StateStore;
import me.wolfii.clienttimers.sound.SoundPlayer;
import me.wolfii.clienttimers.timer.AlarmEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientTimersClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ClientTimers");

    @Override
    public void onInitializeClient() {
        Config.getConfig().ensureDefaults();
        StateStore.loadBlocking();
        ClientAlarmCommands.register();
        ClientTickEvents.END_CLIENT_TICK.register(AlarmEngine::tick);
        Runtime.getRuntime().addShutdownHook(new Thread(AlarmEngine::persistOnShutdown, "clienttimers-shutdown"));
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            SoundPlayer.stopPreview(minecraft);
            SoundPlayer.stopRinging(minecraft);
            AlarmEngine.persistOnShutdown();
        });
    }
}
