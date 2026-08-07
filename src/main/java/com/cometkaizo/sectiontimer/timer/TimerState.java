package com.cometkaizo.sectiontimer.timer;

import com.cometkaizo.sectiontimer.model.TimerProfile;
import com.cometkaizo.sectiontimer.model.TimerSection;

public record TimerState(
        TimerProfile profile,
        TimerSection section,
        int sectionIndex,
        long remainingMillis,
        long totalRemainingMillis,
        double progress,
        boolean started,
        boolean running,
        boolean awaitingConfirmation,
        boolean complete
) {
}
