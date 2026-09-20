package me.wolfii.clienttimers.timer;

import me.wolfii.clienttimers.chat.Notifier;
import me.wolfii.clienttimers.chat.TimerMessages;
import me.wolfii.clienttimers.config.Config;
import me.wolfii.clienttimers.sound.SoundPlayer;
import me.wolfii.clienttimers.persist.StateStore;
import me.wolfii.clienttimers.time.ClockMode;
import me.wolfii.clienttimers.time.DateTimeParser;
import me.wolfii.clienttimers.time.DurationParser;
import me.wolfii.clienttimers.time.ParsedDuration;
import me.wolfii.clienttimers.world.WorldKeys;
import me.wolfii.clienttimers.world.WorldScope;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AlarmEngine {
    public static final String DEFAULT_NAME = "default";
    private static final List<Trackable> ENTRIES = new ArrayList<>();
    private static final List<StoppedNotice> STOPPED_ON_LEAVE = new ArrayList<>();
    private static final AtomicBoolean SHUTDOWN = new AtomicBoolean();
    private static boolean dirty;
    private static long lastSaveMillis = System.currentTimeMillis();
    private static Long lastPlayingAnchor;
    private static Long lastGameAnchor;
    private static String lastWorldKey = "";
    private static boolean wasInWorld;

    private AlarmEngine() {
    }

    public static synchronized void replaceAll(List<Trackable> loaded, List<StoppedNotice> stoppedOnLeave) {
        ENTRIES.clear();
        if (loaded != null) {
            ENTRIES.addAll(loaded);
        }
        STOPPED_ON_LEAVE.clear();
        if (stoppedOnLeave != null) {
            STOPPED_ON_LEAVE.addAll(stoppedOnLeave);
        }
        dirty = false;
        wasInWorld = false;
    }

    public static synchronized List<Trackable> snapshot() {
        return new ArrayList<>(ENTRIES);
    }

    public static synchronized List<StoppedNotice> stoppedOnLeaveSnapshot() {
        return new ArrayList<>(STOPPED_ON_LEAVE);
    }

    public static synchronized List<Trackable> ofKind(TrackableKind kind) {
        List<Trackable> result = new ArrayList<>();
        for (Trackable entry : ENTRIES) {
            if (entry.kind == kind) {
                result.add(entry);
            }
        }
        result.sort(Comparator.comparing(trackable -> trackable.name.toLowerCase(Locale.ROOT)));
        return result;
    }

    public static synchronized Optional<Trackable> find(TrackableKind kind, String name) {
        String resolved = resolveName(name);
        for (Trackable entry : ENTRIES) {
            if (entry.kind == kind && entry.name.equalsIgnoreCase(resolved)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public static String resolveName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_NAME;
        }
        return name;
    }

    public static synchronized void startAlarm(String name, AlarmTarget target, boolean silentOverride) {
        String resolved = resolveName(name);
        Trackable trackable = upsert(TrackableKind.ALARM, resolved);
        resetCommon(trackable, ClockMode.REAL_TIME, WorldScope.ANY_WORLD);
        trackable.silent = silentOverride || trackable.silent;
        applyTarget(trackable, target);
        markDirty();
        Notifier.started(trackable);
    }

    public static synchronized void startTimer(
        String name,
        ParsedDuration duration,
        ClockMode mode,
        WorldScope scope,
        ParsedDuration repeatAfter,
        Integer repeatCount
    ) {
        String resolved = resolveName(name);
        Trackable trackable = upsert(TrackableKind.TIMER, resolved);
        resetCommon(trackable, mode, scope);
        trackable.durationRaw = duration.raw();
        trackable.durationTicks = Math.max(1, duration.ticksFor(mode));
        trackable.durationMillis = Math.max(1, duration.millis());
        trackable.elapsedTicks = 0;
        trackable.elapsedMillis = 0;
        trackable.wallAnchorEpoch = System.currentTimeMillis();
        if (repeatAfter != null && !repeatAfter.isZero()) {
            trackable.hasRepeat = true;
            trackable.repeatRaw = repeatAfter.raw();
            trackable.repeatTicks = Math.max(1, repeatAfter.ticksFor(mode));
            trackable.repeatMillis = Math.max(1, repeatAfter.millis());
            trackable.remainingRepeats = repeatCount == null ? -1 : Math.max(0, repeatCount);
        } else {
            trackable.hasRepeat = false;
            trackable.remainingRepeats = 0;
        }
        markDirty();
        Notifier.started(trackable);
    }

    public static synchronized void toggleStopwatch(String name, ClockMode mode, WorldScope scope) {
        String resolved = resolveName(name);
        Optional<Trackable> existing = find(TrackableKind.STOPWATCH, resolved);
        if (existing.isPresent() && existing.get().running) {
            stop(TrackableKind.STOPWATCH, resolved, true);
            return;
        }
        Trackable trackable = upsert(TrackableKind.STOPWATCH, resolved);
        resetCommon(trackable, mode, scope);
        trackable.elapsedTicks = 0;
        trackable.elapsedMillis = 0;
        trackable.wallAnchorEpoch = System.currentTimeMillis();
        markDirty();
        Notifier.started(trackable);
    }

    public static synchronized boolean stop(TrackableKind kind, String name, boolean notify) {
        Optional<Trackable> found = find(kind, name);
        if (found.isEmpty()) {
            return false;
        }
        Trackable trackable = found.get();
        boolean wasActive = trackable.running || trackable.ringing || trackable.pendingJoinRing;
        deactivate(trackable);
        ENTRIES.remove(trackable);
        markDirty();
        if (notify && wasActive) {
            if (kind == TrackableKind.STOPWATCH) {
                Notifier.info(TimerMessages.ended(trackable));
            } else {
                Notifier.info(TimerMessages.stopped(trackable));
            }
        }
        return true;
    }

    public static synchronized boolean toggleSilent(TrackableKind kind, String name) {
        Optional<Trackable> found = find(kind, name);
        if (found.isEmpty()) {
            return false;
        }
        Trackable trackable = found.get();
        trackable.silent = !trackable.silent;
        if (trackable.silent) {
            trackable.soundCycleTick = -1;
        }
        markDirty();
        Notifier.info(TimerMessages.silent(trackable));
        return true;
    }

    public static synchronized boolean progress(TrackableKind kind, String name) {
        Optional<Trackable> found = find(kind, name);
        if (found.isEmpty()) {
            return false;
        }
        Notifier.info(TimerMessages.progress(found.get()));
        return true;
    }

    public static synchronized int snoozeAll(ParsedDuration duration) {
        long until = System.currentTimeMillis() + Math.max(1000L, duration.millis());
        int count = 0;
        for (Trackable entry : ENTRIES) {
            if (entry.ringing) {
                entry.snoozeUntilEpoch = until;
                entry.soundCycleTick = -1;
                count++;
            }
        }
        if (count > 0) {
            markDirty();
            Notifier.info(TimerMessages.snoozed(DurationParser.format(duration, ClockMode.REAL_TIME)));
        }
        return count;
    }

    public static synchronized boolean hasRinging() {
        long now = System.currentTimeMillis();
        for (Trackable entry : ENTRIES) {
            if (entry.ringing && entry.snoozeUntilEpoch <= now) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void tick(Minecraft minecraft) {
        long now = System.currentTimeMillis();
        boolean inWorld = WorldKeys.inWorld(minecraft);
        if (!inWorld) {
            if (hasRinging()) {
                onLeftWorld(minecraft);
            }
        } else if (!wasInWorld) {
            onJoinedWorld();
        }
        wasInWorld = inWorld;

        String worldKey = WorldKeys.currentWorldKey(minecraft);
        if (!worldKey.equals(lastWorldKey)) {
            lastPlayingAnchor = inWorld ? now : null;
            lastWorldKey = worldKey;
        }
        long playingDelta = 0;
        if (inWorld) {
            if (lastPlayingAnchor == null) {
                lastPlayingAnchor = now;
            }
            playingDelta = now - lastPlayingAnchor;
            lastPlayingAnchor = now;
        } else {
            lastPlayingAnchor = null;
        }
        if (lastGameAnchor == null) {
            lastGameAnchor = now;
        }
        long gameDelta = now - lastGameAnchor;
        lastGameAnchor = now;
        long worldTime = WorldKeys.currentWorldTime(minecraft);
        long worldDay = WorldKeys.currentWorldDay(minecraft);

        List<Trackable> justEnded = new ArrayList<>();
        for (Trackable entry : ENTRIES) {
            if (!entry.running && !entry.ringing && !entry.pendingJoinRing) {
                continue;
            }
            if (entry.ringing && entry.snoozeUntilEpoch > now) {
                continue;
            }
            if (entry.kind == TrackableKind.ALARM) {
                tickAlarm(entry, worldTime, worldDay, now, inWorld, justEnded);
                continue;
            }
            if (!entry.running) {
                continue;
            }
            boolean worldOk = entry.worldScope != WorldScope.THIS_WORLD
                || (entry.worldKey != null && !entry.worldKey.isBlank() && entry.worldKey.equals(worldKey));
            switch (entry.clockMode) {
                case TICKS_PLAYING -> {
                    if (inWorld && worldOk) {
                        entry.elapsedTicks++;
                        entry.elapsedMillis = entry.elapsedTicks * 50L;
                    }
                }
                case TIME_PLAYING -> {
                    if (inWorld && worldOk && playingDelta > 0) {
                        entry.elapsedMillis += playingDelta;
                        entry.elapsedTicks = entry.elapsedMillis / 50L;
                    }
                }
                case GAME_RUNNING -> {
                    if (gameDelta > 0) {
                        entry.elapsedMillis += gameDelta;
                        entry.elapsedTicks = entry.elapsedMillis / 50L;
                    }
                }
                case REAL_TIME -> {
                    if (entry.wallAnchorEpoch != null) {
                        entry.elapsedMillis = now - entry.wallAnchorEpoch;
                        entry.elapsedTicks = entry.elapsedMillis / 50L;
                    }
                }
            }
            if (entry.kind == TrackableKind.TIMER && reached(entry)) {
                completeTimer(entry, justEnded, inWorld);
            }
        }
        for (Trackable ended : justEnded) {
            Notifier.ended(ended, ended.silent || !Config.getConfig().playSounds);
        }
        SoundPlayer.tick(minecraft, inWorld ? ringingEntries() : List.of());
        pruneInactive();
        if (now - lastSaveMillis >= 60_000L && (dirty || hasLiveEntries())) {
            persist();
        }
    }

    public static synchronized void persist() {
        if (SHUTDOWN.get()) {
            return;
        }
        dirty = false;
        lastSaveMillis = System.currentTimeMillis();
        StateStore.saveAsync(snapshot(), stoppedOnLeaveSnapshot());
    }

    public static synchronized void persistBlocking() {
        dirty = false;
        lastSaveMillis = System.currentTimeMillis();
        StateStore.saveBlocking(snapshot(), stoppedOnLeaveSnapshot());
    }

    public static synchronized void persistOnShutdown() {
        if (!SHUTDOWN.compareAndSet(false, true)) {
            return;
        }
        persistBlocking();
        StateStore.shutdown();
    }

    public static synchronized List<String> names(TrackableKind kind) {
        List<String> names = new ArrayList<>();
        names.add(DEFAULT_NAME);
        for (Trackable entry : ofKind(kind)) {
            if (!names.contains(entry.name)) {
                names.add(entry.name);
            }
        }
        return names;
    }

    private static void onLeftWorld(Minecraft minecraft) {
        SoundPlayer.stopRinging(minecraft);
        int stopped = 0;
        for (Trackable entry : new ArrayList<>(ENTRIES)) {
            if (!entry.ringing) {
                continue;
            }
            STOPPED_ON_LEAVE.add(new StoppedNotice(entry.kind, entry.name));
            deactivate(entry);
            ENTRIES.remove(entry);
            stopped++;
        }
        if (stopped > 0) {
            markDirty();
        }
    }

    private static void onJoinedWorld() {
        for (StoppedNotice notice : STOPPED_ON_LEAVE) {
            Notifier.deferred(TimerMessages.stoppedOnLeave(notice));
        }
        if (!STOPPED_ON_LEAVE.isEmpty()) {
            STOPPED_ON_LEAVE.clear();
            markDirty();
        }
        for (Trackable entry : ENTRIES) {
            if (entry.pendingJoinRing) {
                entry.pendingJoinRing = false;
                startRinging(entry);
                Notifier.ended(entry, entry.silent || !Config.getConfig().playSounds);
            }
        }
    }

    private static List<Trackable> ringingEntries() {
        List<Trackable> ringing = new ArrayList<>();
        for (Trackable entry : ENTRIES) {
            if (entry.ringing) {
                ringing.add(entry);
            }
        }
        return ringing;
    }

    private static boolean hasLiveEntries() {
        for (Trackable entry : ENTRIES) {
            if (entry.running || entry.ringing || entry.pendingJoinRing) {
                return true;
            }
        }
        return !STOPPED_ON_LEAVE.isEmpty();
    }

    private static void pruneInactive() {
        Iterator<Trackable> iterator = ENTRIES.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            Trackable entry = iterator.next();
            if (!entry.running && !entry.ringing && !entry.pendingJoinRing) {
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            markDirty();
        }
    }

    private static void tickAlarm(Trackable entry, long worldTime, long worldDay, long now, boolean inWorld, List<Trackable> justEnded) {
        if (entry.ringing || entry.pendingJoinRing) {
            return;
        }
        boolean due = switch (entry.alarmType) {
            case "WALL" -> now >= entry.targetEpochMillis;
            case "GAME_TIME" -> worldTime >= 0 && worldTime >= entry.targetWorldTime;
            case "GAME_DAY" -> worldDay >= 0 && worldDay >= entry.targetDay;
            default -> false;
        };
        boolean available = switch (entry.alarmType) {
            case "WALL" -> true;
            case "GAME_TIME", "GAME_DAY" -> worldTime >= 0;
            default -> false;
        };
        if (due && available) {
            entry.missed = now - entry.createdEpochMillis > 1000 && (
                "WALL".equals(entry.alarmType) && now - entry.targetEpochMillis > 2000
                    || "GAME_TIME".equals(entry.alarmType) && worldTime > entry.targetWorldTime
                    || "GAME_DAY".equals(entry.alarmType) && worldDay > entry.targetDay
            );
            deliverCompletion(entry, inWorld, justEnded);
            entry.running = false;
            entry.completed = true;
        }
    }

    private static boolean reached(Trackable entry) {
        if (entry.clockMode == ClockMode.TICKS_PLAYING) {
            return entry.elapsedTicks >= entry.durationTicks;
        }
        return entry.elapsedMillis >= entry.durationMillis;
    }

    private static void completeTimer(Trackable entry, List<Trackable> justEnded, boolean inWorld) {
        deliverCompletion(entry, inWorld, justEnded);
        if (entry.hasRepeat && entry.remainingRepeats != 0) {
            if (entry.remainingRepeats > 0) {
                entry.remainingRepeats--;
            }
            entry.elapsedTicks = 0;
            entry.elapsedMillis = 0;
            entry.wallAnchorEpoch = System.currentTimeMillis();
            entry.durationTicks = entry.repeatTicks;
            entry.durationMillis = entry.repeatMillis;
            entry.durationRaw = entry.repeatRaw;
            entry.running = true;
            entry.completed = false;
        } else {
            entry.running = false;
            entry.completed = true;
        }
        markDirty();
    }

    private static void deliverCompletion(Trackable entry, boolean inWorld, List<Trackable> justEnded) {
        if (inWorld) {
            if (!entry.ringing) {
                startRinging(entry);
                justEnded.add(entry);
            }
            return;
        }
        entry.pendingJoinRing = true;
        entry.ringing = false;
        entry.soundCycleTick = -1;
        markDirty();
    }

    private static void startRinging(Trackable entry) {
        entry.ringing = true;
        entry.pendingJoinRing = false;
        entry.ringsCompleted = 0;
        entry.soundCycleTick = 0;
        entry.snoozeUntilEpoch = 0;
        markDirty();
    }

    private static void deactivate(Trackable trackable) {
        trackable.running = false;
        trackable.ringing = false;
        trackable.pendingJoinRing = false;
        trackable.completed = true;
        trackable.soundCycleTick = -1;
        trackable.snoozeUntilEpoch = 0;
    }

    private static Trackable upsert(TrackableKind kind, String name) {
        for (Trackable entry : ENTRIES) {
            if (entry.kind == kind && entry.name.equalsIgnoreCase(name)) {
                return entry;
            }
        }
        Trackable created = new Trackable();
        created.kind = kind;
        created.name = name;
        ENTRIES.add(created);
        return created;
    }

    private static void resetCommon(Trackable trackable, ClockMode mode, WorldScope scope) {
        trackable.clockMode = mode;
        trackable.worldScope = mode.supportsWorldScope() ? scope : WorldScope.ANY_WORLD;
        trackable.worldKey = WorldKeys.currentWorldKey(Minecraft.getInstance());
        trackable.silent = Config.getConfig().silentByDefault;
        trackable.running = true;
        trackable.completed = false;
        trackable.missed = false;
        trackable.ringing = false;
        trackable.pendingJoinRing = false;
        trackable.ringsCompleted = 0;
        trackable.snoozeUntilEpoch = 0;
        trackable.soundCycleTick = -1;
        trackable.createdEpochMillis = System.currentTimeMillis();
        trackable.hasRepeat = false;
        trackable.remainingRepeats = 0;
    }

    private static void applyTarget(Trackable trackable, AlarmTarget target) {
        switch (target) {
            case AlarmTarget.WallTime wallTime -> {
                trackable.alarmType = "WALL";
                trackable.targetEpochMillis = wallTime.when().toInstant().toEpochMilli();
                trackable.targetDisplay = DateTimeParser.formatWall(wallTime.when(), Config.getConfig().dateOrder);
                trackable.durationMillis = Math.max(0, trackable.targetEpochMillis - System.currentTimeMillis());
                trackable.clockMode = ClockMode.REAL_TIME;
            }
            case AlarmTarget.GameTime gameTime -> {
                trackable.alarmType = "GAME_TIME";
                trackable.targetWorldTime = gameTime.worldTime();
                trackable.targetDisplay = gameTime.worldTime() + "t";
                trackable.clockMode = ClockMode.TICKS_PLAYING;
            }
            case AlarmTarget.GameDay gameDay -> {
                trackable.alarmType = "GAME_DAY";
                trackable.targetDay = gameDay.day();
                trackable.targetDisplay = Component.translatable("clienttimers.value.day", gameDay.day()).getString();
                trackable.clockMode = ClockMode.TICKS_PLAYING;
            }
        }
    }

    private static void markDirty() {
        dirty = true;
    }
}
