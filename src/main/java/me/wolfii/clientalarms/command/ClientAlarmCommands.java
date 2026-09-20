package me.wolfii.clientalarms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.wolfii.clientalarms.config.Config;
import me.wolfii.clientalarms.engine.AlarmEngine;
import me.wolfii.clientalarms.engine.Trackable;
import me.wolfii.clientalarms.engine.TrackableKind;
import me.wolfii.clientalarms.notify.MessageFormats;
import me.wolfii.clientalarms.notify.Notifier;
import me.wolfii.clientalarms.time.AlarmTarget;
import me.wolfii.clientalarms.time.ClockMode;
import me.wolfii.clientalarms.time.DateTimeParser;
import me.wolfii.clientalarms.time.DurationParser;
import me.wolfii.clientalarms.time.ParsedDuration;
import me.wolfii.clientalarms.time.WorldKeys;
import me.wolfii.clientalarms.time.WorldScope;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ClientAlarmCommands {
    private static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(Component.translatable("clientalarms.error.notFound"));
    private static final SimpleCommandExceptionType RESERVED = new SimpleCommandExceptionType(Component.translatable("clientalarms.error.reservedName"));
    private static final SimpleCommandExceptionType BAD_SCOPE = new SimpleCommandExceptionType(Component.translatable("clientalarms.error.worldScope"));
    private static final SimpleCommandExceptionType NOTHING_RINGING = new SimpleCommandExceptionType(Component.translatable("clientalarms.error.nothingRinging"));
    private static final SimpleCommandExceptionType ALARM_PAST = new SimpleCommandExceptionType(Component.translatable("clientalarms.error.alarmInPast"));
    private static final SimpleCommandExceptionType NOT_IN_WORLD = new SimpleCommandExceptionType(Component.translatable("clientalarms.error.notInWorld"));

    private ClientAlarmCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ClientAlarmCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext ignored) {
        dispatcher.register(buildAlarm());
        dispatcher.register(buildTimer());
        dispatcher.register(buildStopwatch());
        dispatcher.register(buildSnooze());
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAlarm() {
        var command = ClientCommands.literal("calarm");
        attachActions(command, TrackableKind.ALARM, true);
        command.then(ClientCommands.argument("when", StringArgumentType.string())
                .suggests((context, builder) -> SuggestionsUtil.alarmTimes(builder))
                .executes(context -> createAlarm(context, AlarmEngine.DEFAULT_NAME))
                .then(ClientCommands.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> SuggestionsUtil.names(TrackableKind.ALARM, builder))
                        .executes(context -> createAlarm(context, StringArgumentType.getString(context, "name")))));
        return command;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildTimer() {
        var command = ClientCommands.literal("ctimer");
        attachActions(command, TrackableKind.TIMER, true);
        command.then(ClientCommands.argument("duration", StringArgumentType.string())
                .suggests((context, builder) -> SuggestionsUtil.durations(builder))
                .executes(context -> createTimer(context, AlarmEngine.DEFAULT_NAME, ClockMode.TIME_PLAYING, WorldScope.ANY_WORLD, null, null))
                .then(ClientCommands.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> SuggestionsUtil.names(TrackableKind.TIMER, builder))
                        .executes(context -> createTimer(context, StringArgumentType.getString(context, "name"), ClockMode.TIME_PLAYING, WorldScope.ANY_WORLD, null, null))
                        .then(timerModeNode())));
        return command;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<FabricClientCommandSource, String> timerModeNode() {
        var mode = ClientCommands.argument("mode", StringArgumentType.word())
                .suggests((context, builder) -> SuggestionsUtil.clockModes(builder))
                .executes(context -> createTimer(context, StringArgumentType.getString(context, "name"), mode(context), WorldScope.ANY_WORLD, null, null))
                .then(repeatNode(WorldScope.ANY_WORLD));
        mode.then(ClientCommands.literal(WorldScope.THIS_WORLD.commandName())
                .executes(context -> createTimer(context, StringArgumentType.getString(context, "name"), mode(context), WorldScope.THIS_WORLD, null, null))
                .then(repeatNode(WorldScope.THIS_WORLD)));
        mode.then(ClientCommands.literal(WorldScope.ANY_WORLD.commandName())
                .executes(context -> createTimer(context, StringArgumentType.getString(context, "name"), mode(context), WorldScope.ANY_WORLD, null, null))
                .then(repeatNode(WorldScope.ANY_WORLD)));
        return mode;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<FabricClientCommandSource, String> repeatNode(WorldScope scope) {
        return ClientCommands.argument("repeatAfter", StringArgumentType.string())
                .suggests((context, builder) -> SuggestionsUtil.durations(builder))
                .executes(context -> createTimer(
                        context,
                        StringArgumentType.getString(context, "name"),
                        mode(context),
                        scope,
                        durationArg(context, "repeatAfter"),
                        null
                ))
                .then(ClientCommands.argument("count", IntegerArgumentType.integer(0))
                        .executes(context -> createTimer(
                                context,
                                StringArgumentType.getString(context, "name"),
                                mode(context),
                                scope,
                                durationArg(context, "repeatAfter"),
                                IntegerArgumentType.getInteger(context, "count")
                        )));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildStopwatch() {
        var command = ClientCommands.literal("cstopwatch")
                .executes(context -> toggleStopwatch(AlarmEngine.DEFAULT_NAME, ClockMode.TIME_PLAYING, WorldScope.ANY_WORLD));
        attachActions(command, TrackableKind.STOPWATCH, false);
        command.then(ClientCommands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> SuggestionsUtil.names(TrackableKind.STOPWATCH, builder))
                .executes(context -> toggleStopwatch(requireName(StringArgumentType.getString(context, "name")), ClockMode.TIME_PLAYING, WorldScope.ANY_WORLD))
                .then(ClientCommands.argument("mode", StringArgumentType.word())
                        .suggests((context, builder) -> SuggestionsUtil.clockModes(builder))
                        .executes(context -> toggleStopwatch(requireName(StringArgumentType.getString(context, "name")), mode(context), WorldScope.ANY_WORLD))
                        .then(ClientCommands.literal(WorldScope.THIS_WORLD.commandName())
                                .executes(context -> toggleStopwatch(requireName(StringArgumentType.getString(context, "name")), mode(context), WorldScope.THIS_WORLD)))
                        .then(ClientCommands.literal(WorldScope.ANY_WORLD.commandName())
                                .executes(context -> toggleStopwatch(requireName(StringArgumentType.getString(context, "name")), mode(context), WorldScope.ANY_WORLD)))));
        return command;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildSnooze() {
        return ClientCommands.literal("csnooze")
                .executes(context -> snooze(DurationParser.parse("5min")))
                .then(ClientCommands.argument("duration", StringArgumentType.string())
                        .suggests((context, builder) -> SuggestionsUtil.durations(builder))
                        .executes(context -> snooze(durationArg(context, "duration"))));
    }

    private static void attachActions(LiteralArgumentBuilder<FabricClientCommandSource> command, TrackableKind kind, boolean includeSilent) {
        command.then(ClientCommands.literal("list").executes(context -> list(kind)));
        attachNamed(command, "stop", kind, name -> AlarmEngine.stop(kind, name, true) ? 1 : fail());
        attachNamed(command, "progress", kind, name -> AlarmEngine.progress(kind, name) ? 1 : fail());
        attachNamed(command, "hide", kind, name -> AlarmEngine.setOverlay(kind, name, false) ? 1 : fail());
        attachNamed(command, "show", kind, name -> AlarmEngine.setOverlay(kind, name, true) ? 1 : fail());
        if (includeSilent) {
            attachNamed(command, "silent", kind, name -> AlarmEngine.toggleSilent(kind, name) ? 1 : fail());
        }
    }

    private interface NamedAction {
        int run(String name) throws CommandSyntaxException;
    }

    private static void attachNamed(LiteralArgumentBuilder<FabricClientCommandSource> command, String literal, TrackableKind kind, NamedAction action) {
        command.then(ClientCommands.literal(literal)
                .executes(context -> action.run(AlarmEngine.DEFAULT_NAME))
                .then(ClientCommands.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> SuggestionsUtil.names(kind, builder))
                        .executes(context -> action.run(StringArgumentType.getString(context, "name")))));
    }

    private static int fail() throws CommandSyntaxException {
        throw UNKNOWN.create();
    }

    private static String requireName(String name) throws CommandSyntaxException {
        if (CommandTokens.isReservedName(name)) {
            throw RESERVED.create();
        }
        return name;
    }

    private static void requireFuture(AlarmTarget target) throws CommandSyntaxException {
        Minecraft minecraft = Minecraft.getInstance();
        switch (target) {
            case AlarmTarget.WallTime wallTime -> {
                if (!wallTime.when().isAfter(DateTimeParser.now())) {
                    throw ALARM_PAST.create();
                }
            }
            case AlarmTarget.GameTime gameTime -> {
                long worldTime = WorldKeys.currentWorldTime(minecraft);
                if (worldTime >= 0 && gameTime.worldTime() <= worldTime) {
                    throw ALARM_PAST.create();
                }
            }
            case AlarmTarget.GameDay gameDay -> {
                long worldDay = WorldKeys.currentWorldDay(minecraft);
                if (worldDay >= 0 && gameDay.day() <= worldDay) {
                    throw ALARM_PAST.create();
                }
            }
        }
    }

    private static void requireWorldForScope(WorldScope scope) throws CommandSyntaxException {
        if (scope == WorldScope.THIS_WORLD && WorldKeys.currentWorldKey(Minecraft.getInstance()).isBlank()) {
            throw NOT_IN_WORLD.create();
        }
    }

    private static int list(TrackableKind kind) {
        List<Trackable> entries = AlarmEngine.ofKind(kind);
        if (entries.isEmpty()) {
            Notifier.list(Component.translatable("clientalarms.list.empty." + kind.name().toLowerCase()));
            return 1;
        }
        Notifier.list(Component.translatable("clientalarms.list.header." + kind.name().toLowerCase()));
        for (Trackable entry : entries) {
            Notifier.list(Component.literal(" - " + MessageFormats.overlayLine(entry)));
        }
        return 1;
    }

    private static int createAlarm(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        requireName(name);
        try {
            AlarmTarget target = DateTimeParser.parse(StringArgumentType.getString(context, "when"), Config.get().dateOrder, DateTimeParser.now());
            requireFuture(target);
            AlarmEngine.startAlarm(name, target, false);
            return 1;
        } catch (IllegalArgumentException exception) {
            throw new SimpleCommandExceptionType(Component.translatable("clientalarms.error.invalidTime", exception.getMessage())).create();
        }
    }

    private static int createTimer(
            CommandContext<FabricClientCommandSource> context,
            String name,
            ClockMode mode,
            WorldScope scope,
            ParsedDuration repeatAfter,
            Integer repeatCount
    ) throws CommandSyntaxException {
        requireName(name);
        if (!mode.supportsWorldScope() && scope == WorldScope.THIS_WORLD) {
            throw BAD_SCOPE.create();
        }
        requireWorldForScope(scope);
        try {
            ParsedDuration duration = DurationParser.parse(StringArgumentType.getString(context, "duration"));
            AlarmEngine.startTimer(name, duration, mode, scope, repeatAfter, repeatCount);
            return 1;
        } catch (IllegalArgumentException exception) {
            throw new SimpleCommandExceptionType(Component.translatable("clientalarms.error.invalidDuration", exception.getMessage())).create();
        }
    }

    private static int toggleStopwatch(String name, ClockMode mode, WorldScope scope) throws CommandSyntaxException {
        if (!mode.supportsWorldScope() && scope == WorldScope.THIS_WORLD) {
            throw BAD_SCOPE.create();
        }
        requireWorldForScope(scope);
        AlarmEngine.toggleStopwatch(name, mode, scope);
        return 1;
    }

    private static int snooze(ParsedDuration duration) throws CommandSyntaxException {
        int count = AlarmEngine.snoozeAll(duration);
        if (count == 0) {
            throw NOTHING_RINGING.create();
        }
        return count;
    }

    private static ClockMode mode(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        try {
            return ClockMode.fromCommand(StringArgumentType.getString(context, "mode"));
        } catch (IllegalArgumentException exception) {
            throw new SimpleCommandExceptionType(Component.translatable("clientalarms.error.invalidMode")).create();
        }
    }

    private static ParsedDuration durationArg(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        try {
            return DurationParser.parse(StringArgumentType.getString(context, name));
        } catch (IllegalArgumentException exception) {
            throw new SimpleCommandExceptionType(Component.translatable("clientalarms.error.invalidDuration", exception.getMessage())).create();
        }
    }
}
