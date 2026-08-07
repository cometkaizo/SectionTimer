package com.cometkaizo.sectiontimer.model;

import java.util.ArrayList;
import java.util.List;

public final class TimerData {
    private List<TimerProfile> profiles = new ArrayList<>();
    private int currentProfileIndex;

    public TimerData() {
    }

    public TimerData(List<TimerProfile> profiles, int currentProfileIndex) {
        this.profiles = new ArrayList<>(profiles);
        this.currentProfileIndex = currentProfileIndex;
    }

    public List<TimerProfile> getProfiles() {
        if (profiles == null) {
            profiles = new ArrayList<>();
        }
        return profiles;
    }

    public void setProfiles(List<TimerProfile> profiles) {
        this.profiles = profiles == null ? new ArrayList<>() : new ArrayList<>(profiles);
    }

    public int getCurrentProfileIndex() {
        return currentProfileIndex;
    }

    public void setCurrentProfileIndex(int currentProfileIndex) {
        this.currentProfileIndex = currentProfileIndex;
    }
}
