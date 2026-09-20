package me.wolfii.clienttimers.world;

public enum WorldScope {
    THIS_WORLD,
    ANY_WORLD;

    public String commandName() {
        return name().toLowerCase();
    }
}
