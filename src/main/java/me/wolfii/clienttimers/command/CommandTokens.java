package me.wolfii.clienttimers.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CommandTokens {
    public static final Set<String> RESERVED_NAMES = Set.of("stop", "progress", "hide", "show", "list", "silent");

    private CommandTokens() {
    }

    public static boolean isReservedName(String name) {
        return name != null && RESERVED_NAMES.contains(name.toLowerCase());
    }

    public static List<String> actions(boolean includeSilent) {
        List<String> actions = new ArrayList<>(List.of("stop", "progress", "hide", "show"));
        if (includeSilent) {
            actions.add("silent");
        }
        return actions;
    }
}
