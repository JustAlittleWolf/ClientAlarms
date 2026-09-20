package me.wolfii.clienttimers.time;

public enum WorldScope {
    THIS_WORLD,
    ANY_WORLD;

    public String commandName() {
        return name().toLowerCase();
    }
}
