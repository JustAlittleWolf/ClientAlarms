package me.wolfii.clienttimers.engine;

import me.wolfii.clienttimers.time.ClockMode;
import me.wolfii.clienttimers.time.WorldScope;

public class Trackable {
    public TrackableKind kind = TrackableKind.TIMER;
    public String name = "default";
    public boolean overlayVisible = true;
    public boolean silent = false;
    public ClockMode clockMode = ClockMode.TIME_PLAYING;
    public WorldScope worldScope = WorldScope.ANY_WORLD;
    public String worldKey = "";

    public String alarmType = "";
    public long targetEpochMillis;
    public long targetWorldTime;
    public long targetDay;
    public String targetDisplay = "";

    public long durationTicks;
    public long durationMillis;
    public String durationRaw = "";
    public boolean hasRepeat;
    public long repeatTicks;
    public long repeatMillis;
    public String repeatRaw = "";
    public int remainingRepeats = -1;

    public long elapsedTicks;
    public long elapsedMillis;
    public Long wallAnchorEpoch;
    public boolean running = true;
    public boolean completed;
    public boolean missed;
    public boolean ringing;
    public int ringsCompleted;
    public long snoozeUntilEpoch;
    public int soundCycleTick = -1;
    public long createdEpochMillis = System.currentTimeMillis();
}
