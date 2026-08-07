package com.cometkaizo.sectiontimer.model;

import java.util.Objects;

public final class TimerSection {
    private String name;
    private int durationSeconds;
    private String colorHex;
    private int lowTimeWarningPercent;
    private Boolean waitForConfirmation;

    public TimerSection() {
        // Required by Gson.
    }

    public TimerSection(String name, int durationSeconds, String colorHex) {
        this(name, durationSeconds, colorHex, 0);
    }

    public TimerSection(String name, int durationSeconds, String colorHex,
                        int lowTimeWarningPercent) {
        this(name, durationSeconds, colorHex, lowTimeWarningPercent, true);
    }

    public TimerSection(String name, int durationSeconds, String colorHex,
                        int lowTimeWarningPercent, boolean waitForConfirmation) {
        this.name = Objects.requireNonNull(name);
        this.durationSeconds = durationSeconds;
        this.colorHex = Objects.requireNonNull(colorHex);
        this.lowTimeWarningPercent = lowTimeWarningPercent;
        this.waitForConfirmation = waitForConfirmation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public int getLowTimeWarningPercent() {
        return lowTimeWarningPercent;
    }

    public void setLowTimeWarningPercent(int lowTimeWarningPercent) {
        this.lowTimeWarningPercent = Math.max(0, Math.min(100, lowTimeWarningPercent));
    }

    public boolean isWaitForConfirmation() {
        return waitForConfirmation == null || waitForConfirmation;
    }

    public void setWaitForConfirmation(boolean waitForConfirmation) {
        this.waitForConfirmation = waitForConfirmation;
    }
}
