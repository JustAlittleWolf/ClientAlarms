package me.wolfii.clienttimers.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.wolfii.clienttimers.timer.AlarmEngine;
import me.wolfii.clienttimers.timer.TrackableKind;
import me.wolfii.clienttimers.time.ClockMode;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SuggestionsUtil {
    private SuggestionsUtil() {
    }

    public static CompletableFuture<Suggestions> names(TrackableKind kind, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(AlarmEngine.names(kind), builder);
    }

    public static CompletableFuture<Suggestions> durations(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of("20t", "10s", "5min", "1h", "1d"), builder);
    }

    public static CompletableFuture<Suggestions> alarmTimes(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of("18:00", "2:00pm", "1000t", "1d"), builder);
    }

    public static CompletableFuture<Suggestions> clockModes(SuggestionsBuilder builder) {
        List<String> names = List.of(
            ClockMode.TICKS_PLAYING.commandName(),
            ClockMode.TIME_PLAYING.commandName(),
            ClockMode.GAME_RUNNING.commandName(),
            ClockMode.REAL_TIME.commandName()
        );
        return SharedSuggestionProvider.suggest(names, builder);
    }
}
