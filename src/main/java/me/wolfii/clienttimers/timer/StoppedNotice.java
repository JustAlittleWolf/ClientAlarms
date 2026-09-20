package me.wolfii.clienttimers.timer;

public class StoppedNotice {
    public TrackableKind kind = TrackableKind.ALARM;
    public String name = "";

    public StoppedNotice() {
    }

    public StoppedNotice(TrackableKind kind, String name) {
        this.kind = kind;
        this.name = name;
    }
}
