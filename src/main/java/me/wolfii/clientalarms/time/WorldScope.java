package me.wolfii.clientalarms.time;

public enum WorldScope {
    THIS_WORLD,
    ANY_WORLD;

    public String commandName() {
        return name().toLowerCase();
    }

    public static WorldScope fromCommand(String value) {
        return WorldScope.valueOf(value.toUpperCase());
    }
}
