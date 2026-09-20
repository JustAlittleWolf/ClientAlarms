package me.wolfii.clienttimers.command;

import java.util.Set;

public final class CommandTokens {
    public static final Set<String> ALARM_ACTIONS = Set.of("stop", "progress", "hide", "show", "list", "silent");
    public static final Set<String> TIMER_ACTIONS = Set.of("stop", "progress", "hide", "show", "list", "silent");
    public static final Set<String> STOPWATCH_ACTIONS = Set.of("stop", "progress", "hide", "show", "list");
    public static final Set<String> RESERVED_NAMES = Set.of("stop", "progress", "hide", "show", "list", "silent");

    private CommandTokens() {
    }

    public static boolean isReservedName(String name) {
        return name != null && RESERVED_NAMES.contains(name.toLowerCase());
    }
}
