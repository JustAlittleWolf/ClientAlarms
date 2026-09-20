package me.wolfii.clienttimers.time;

public enum WorldScope {
    THIS_WORLD,
    ANY_WORLD;

    public static WorldScope fromCommand(String value) {
        return WorldScope.valueOf(value.toUpperCase());
    }

    public String commandName() {
        return name().toLowerCase();
    }
}
