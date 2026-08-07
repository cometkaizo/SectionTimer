package com.cometkaizo.sectiontimer.ui;

import com.cometkaizo.sectiontimer.model.TimerProfile;
import com.cometkaizo.sectiontimer.model.TimerSection;
import com.cometkaizo.sectiontimer.timer.TimerState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

final class MiniSessionBar extends JPanel {
    private static final int BAR_X = 6;
    private static final int BAR_WIDTH = 12;
    private TimerState state;

    MiniSessionBar() {
        setOpaque(false);
        setPreferredSize(new Dimension(32, 140));
    }

    void setTimerState(TimerState state) {
        this.state = state;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (state == null || state.profile() == null || state.profile().getSections().isEmpty()) return;

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            List<TimerSection> sections = state.profile().getSections();
            long total = totalSeconds(state.profile());
            int top = 12;
            int height = Math.max(1, getHeight() - 16);
            double y = top;
            for (int i = 0; i < sections.size(); i++) {
                TimerSection section = sections.get(i);
                double sectionHeight = i == sections.size() - 1
                        ? top + height - y
                        : height * section.getDurationSeconds() / (double) total;
                g2.setColor(ColorUtil.fromHex(section.getColorHex()));
                g2.fillRoundRect(BAR_X, (int) Math.round(y), BAR_WIDTH,
                        Math.max(2, (int) Math.ceil(sectionHeight)), 6, 6);

                y += sectionHeight;
            }

            double overallProgress = overallProgress(state, total);
            double markerY = top + overallProgress * height;
            g2.setColor(Theme.TEXT);
            Path2D marker = new Path2D.Double();
            marker.moveTo(18, markerY);
            marker.lineTo(25, markerY - 5);
            marker.lineTo(25, markerY + 5);
            marker.closePath();
            g2.fill(marker);
        } finally {
            g2.dispose();
        }
    }

    private long totalSeconds(TimerProfile profile) {
        return Math.max(1, profile.getSections().stream()
                .mapToLong(TimerSection::getDurationSeconds).sum());
    }

    private double overallProgress(TimerState timerState, long total) {
        long completed = 0;
        List<TimerSection> sections = timerState.profile().getSections();
        for (int i = 0; i < timerState.sectionIndex(); i++) completed += sections.get(i).getDurationSeconds();
        long currentDuration = timerState.section() == null ? 0 : timerState.section().getDurationSeconds();
        double elapsed = completed + currentDuration * timerState.progress();
        return Math.max(0.0, Math.min(1.0, elapsed / total));
    }

}
