package com.cometkaizo.sectiontimer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TimerProfile {
    private String name;
    private List<TimerSection> sections = new ArrayList<>();

    public TimerProfile() {
        // Required by Gson.
    }

    public TimerProfile(String name, List<TimerSection> sections) {
        this.name = Objects.requireNonNull(name);
        this.sections = new ArrayList<>(Objects.requireNonNull(sections));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TimerSection> getSections() {
        if (sections == null) {
            sections = new ArrayList<>();
        }
        return sections;
    }

    public void setSections(List<TimerSection> sections) {
        this.sections = sections == null ? new ArrayList<>() : new ArrayList<>(sections);
    }
}
