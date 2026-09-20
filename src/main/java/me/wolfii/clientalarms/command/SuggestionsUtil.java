package me.wolfii.clientalarms.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.wolfii.clientalarms.engine.AlarmEngine;
import me.wolfii.clientalarms.engine.TrackableKind;
import me.wolfii.clientalarms.time.ClockMode;
import me.wolfii.clientalarms.time.WorldScope;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class SuggestionsUtil {
    private SuggestionsUtil() {
    }

    public static CompletableFuture<Suggestions> names(TrackableKind kind, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(AlarmEngine.names(kind), builder);
    }

    public static CompletableFuture<Suggestions> durations(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of("10s", "30s", "1min", "5min", "10min", "1h", "1h30min", "20t", "1d"), builder);
    }

    public static CompletableFuture<Suggestions> alarmTimes(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of(
                "12:00",
                "18:00",
                "2:00pm",
                "9:00am",
                "1000t",
                "1d",
                "5d"
        ), builder);
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

    public static CompletableFuture<Suggestions> worldScopes(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of(
                WorldScope.THIS_WORLD.commandName(),
                WorldScope.ANY_WORLD.commandName()
        ), builder);
    }

    public static CompletableFuture<Suggestions> actions(Iterable<String> actions, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(actions, builder);
    }

    public static boolean matches(String remaining, String value) {
        return value.toLowerCase(Locale.ROOT).startsWith(remaining.toLowerCase(Locale.ROOT));
    }
}
