package com.cometkaizo.sectiontimer.timer;

import com.cometkaizo.sectiontimer.model.TimerProfile;
import com.cometkaizo.sectiontimer.model.TimerSection;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class TimerEngine {
    private static final int UPDATE_INTERVAL_MILLIS = 50;

    private final Timer swingTimer;
    private final Timer confirmationWarningTimer;
    private final Consumer<TimerState> listener;
    private final Runnable lowTimeWarningAction;
    private final Runnable endAction;

    private TimerProfile profile;
    private int sectionIndex;
    private long remainingNanos;
    private long sectionEndNanos;
    private boolean running;
    private boolean started;
    private boolean complete;
    private boolean lowTimeWarningPlayed;
    private boolean awaitingConfirmation;

    public TimerEngine(Consumer<TimerState> listener) {
        this(listener, () -> { }, () -> { });
    }

    public TimerEngine(Consumer<TimerState> listener, Runnable lowTimeWarningAction) {
        this(listener, lowTimeWarningAction, () -> { });
    }

    public TimerEngine(Consumer<TimerState> listener, Runnable lowTimeWarningAction,
                       Runnable endAction) {
        this.listener = Objects.requireNonNull(listener);
        this.lowTimeWarningAction = Objects.requireNonNull(lowTimeWarningAction);
        this.endAction = Objects.requireNonNull(endAction);
        this.swingTimer = new Timer(UPDATE_INTERVAL_MILLIS, event -> tick());
        this.swingTimer.setCoalesce(true);
        this.confirmationWarningTimer = new Timer(1_500,
                event -> this.endAction.run());
        this.confirmationWarningTimer.setCoalesce(true);
    }

    public void setProfile(TimerProfile profile) {
        swingTimer.stop();
        this.profile = profile;
        this.running = false;
        this.started = false;
        this.complete = false;
        this.sectionIndex = 0;
        this.lowTimeWarningPlayed = false;
        this.awaitingConfirmation = false;
        confirmationWarningTimer.stop();

        if (hasSections()) {
            this.remainingNanos = durationNanos(currentSection());
        } else {
            this.remainingNanos = 0;
        }
        publishState();
    }

    public void toggleRunning() {
        if (!hasSections()) {
            return;
        }

        if (awaitingConfirmation) {
            return;
        }

        if (complete) {
            reset();
        }

        if (running) {
            pause();
        } else {
            start();
        }
    }

    public void handleTimerCircleClick() {
        if (awaitingConfirmation) {
            confirmSectionCompletion();
        } else {
            toggleRunning();
        }
    }

    public void reset() {
        swingTimer.stop();
        running = false;
        started = false;
        complete = false;
        sectionIndex = 0;
        lowTimeWarningPlayed = false;
        awaitingConfirmation = false;
        confirmationWarningTimer.stop();
        remainingNanos = hasSections() ? durationNanos(currentSection()) : 0;
        publishState();
    }

    public void stop() {
        swingTimer.stop();
        confirmationWarningTimer.stop();
        awaitingConfirmation = false;
        running = false;
    }

    private void start() {
        if (remainingNanos <= 0) {
            remainingNanos = durationNanos(currentSection());
        }
        running = true;
        started = true;
        sectionEndNanos = System.nanoTime() + remainingNanos;
        swingTimer.start();
        publishState();
    }

    private void pause() {
        remainingNanos = Math.max(0, sectionEndNanos - System.nanoTime());
        running = false;
        swingTimer.stop();
        publishState();
    }

    private void tick() {
        if (!running || !hasSections()) {
            return;
        }

        long now = System.nanoTime();
        while (now >= sectionEndNanos && running) {
            remainingNanos = 0;
            endAction.run();
            if (currentSection().isWaitForConfirmation()) {
                beginConfirmationWait();
            } else if (sectionIndex + 1 < profile.getSections().size()) {
                sectionIndex++;
                lowTimeWarningPlayed = false;
                remainingNanos = durationNanos(currentSection());
                sectionEndNanos += remainingNanos;
            } else {
                remainingNanos = 0;
                running = false;
                complete = true;
                swingTimer.stop();
            }
        }

        if (running) {
            remainingNanos = Math.max(0, sectionEndNanos - now);
            playLowTimeWarningIfNeeded();
        }
        publishState();
    }

    private void beginConfirmationWait() {
        running = false;
        awaitingConfirmation = true;
        swingTimer.stop();
        confirmationWarningTimer.restart();
    }

    private void confirmSectionCompletion() {
        confirmationWarningTimer.stop();
        awaitingConfirmation = false;

        if (sectionIndex + 1 < profile.getSections().size()) {
            sectionIndex++;
            lowTimeWarningPlayed = false;
            remainingNanos = durationNanos(currentSection());
            running = true;
            sectionEndNanos = System.nanoTime() + remainingNanos;
            swingTimer.start();
        } else {
            remainingNanos = 0;
            running = false;
            complete = true;
        }
        publishState();
    }

    private void playLowTimeWarningIfNeeded() {
        int warningPercent = currentSection().getLowTimeWarningPercent();
        if (lowTimeWarningPlayed || warningPercent <= 0) return;

        double remainingFraction = (double) remainingNanos / durationNanos(currentSection());
        if (remainingFraction <= warningPercent / 100.0) {
            lowTimeWarningPlayed = true;
            lowTimeWarningAction.run();
        }
    }

    private void publishState() {
        if (!hasSections()) {
            listener.accept(new TimerState(profile, null, 0, 0, 0, 0,
                    false, false, false, false));
            return;
        }

        TimerSection section = currentSection();
        long totalNanos = durationNanos(section);
        double progress = totalNanos == 0
                ? 0
                : 1.0 - (double) remainingNanos / totalNanos;
        progress = Math.max(0.0, Math.min(1.0, progress));

        long remainingMillis = nanosToDisplayMillis(remainingNanos);
        long totalRemainingNanos = remainingNanos;
        for (int i = sectionIndex + 1; i < profile.getSections().size(); i++) {
            totalRemainingNanos += durationNanos(profile.getSections().get(i));
        }
        long totalRemainingMillis = nanosToDisplayMillis(totalRemainingNanos);
        listener.accept(new TimerState(
                profile,
                section,
                sectionIndex,
                remainingMillis,
                totalRemainingMillis,
                progress,
                started,
                running,
                awaitingConfirmation,
                complete
        ));
    }

    private boolean hasSections() {
        return profile != null && !profile.getSections().isEmpty();
    }

    private TimerSection currentSection() {
        List<TimerSection> sections = profile.getSections();
        return sections.get(Math.max(0, Math.min(sectionIndex, sections.size() - 1)));
    }

    private long durationNanos(TimerSection section) {
        return section.getDurationSeconds() * 1_000_000_000L;
    }

    private long nanosToDisplayMillis(long nanos) {
        if (nanos <= 0) {
            return 0;
        }
        return (nanos + 999_999L) / 1_000_000L;
    }
}
