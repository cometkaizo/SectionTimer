package com.cometkaizo.sectiontimer.ui;

import com.cometkaizo.sectiontimer.timer.TimerState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.lang.Math.max;

public final class CircularTimerPanel extends JPanel {
    private static final int OUTER_PADDING = 34;
    private static final int STROKE_WIDTH = 18, COMPACT_STROKE_WIDTH = 9;
    private static final DateTimeFormatter END_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    public static final long FLASH_PHASE_DURATION = 200_000_000L, FLASH_PERIOD = FLASH_PHASE_DURATION * 2;

    private TimerState state;
    private TimerState outgoingState;
    private int pendingSlideDirection;
    private int slideDirection = 1;
    private double slideProgress = 1.0;
    private double pressScale = 1.0;
    private double targetPressScale = 1.0;
    private double totalTimeOpacity = 0.0;
    private double targetTotalTimeOpacity = 0.0;
    private Runnable editAction;
    private boolean compactMode;
    private final Timer animationTimer = new Timer(1_000 / 60, event -> animate());

    public CircularTimerPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(430, 430));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        animationTimer.setCoalesce(true);

        loadSounds();

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                Assets.sound("click_down").play();
                if (event.getButton() == MouseEvent.BUTTON1 && containsCircle(event.getX(), event.getY())) {
                    targetPressScale = 0.955;
                    startAnimation();
                }
            }
            @Override public void mouseReleased(MouseEvent event) {
                Assets.sound("click_up").play();
                if (event.getButton() == MouseEvent.BUTTON1) {
                    targetPressScale = 1.0;
                    startAnimation();
                } else if (event.getButton() == MouseEvent.BUTTON3
                        && !compactMode
                        && containsCircle(event.getX(), event.getY())
                        && editAction != null) {
                    editAction.run();
                }
            }
            @Override public void mouseExited(MouseEvent event) {
                targetPressScale = 1.0;
                targetTotalTimeOpacity = 0.0;
                startAnimation();
            }
            @Override public void mouseEntered(MouseEvent event) {
                if (compactMode) {
                    targetTotalTimeOpacity = 1.0;
                    startAnimation();
                }
            }
        });
    }

    private void loadSounds() {
        Assets.sound("click_down");
        Assets.sound("click_up");
    }

    public void setEditAction(Runnable editAction) {
        this.editAction = editAction;
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
        targetTotalTimeOpacity = compactMode ? 0.0 : 1.0;
        totalTimeOpacity = compactMode ? 0.0 : 1.0;
        repaint();
    }

    public void prepareProfileTransition(int direction) {
        pendingSlideDirection = Integer.signum(direction);
    }

    public void setTimerState(TimerState state) {
        if (pendingSlideDirection != 0 && this.state != null) {
            outgoingState = this.state;
            slideDirection = pendingSlideDirection;
            slideProgress = 0.0;
            startAnimation();
        }
        this.state = state;
        pendingSlideDirection = 0;
        if (state != null && state.awaitingConfirmation()) {
            startAnimation();
        }
        repaint();
    }

    private void startAnimation() {
        if (!animationTimer.isRunning()) animationTimer.start();
    }

    private void animate() {
        pressScale += (targetPressScale - pressScale) * 0.28;
        totalTimeOpacity += (targetTotalTimeOpacity - totalTimeOpacity) * 0.18;
        if (slideProgress < 1.0) slideProgress = Math.min(1.0, slideProgress + 0.075);
        if (Math.abs(targetPressScale - pressScale) < 0.002) pressScale = targetPressScale;
        if (Math.abs(targetTotalTimeOpacity - totalTimeOpacity) < 0.01) {
            totalTimeOpacity = targetTotalTimeOpacity;
        }
        if (slideProgress >= 1.0) outgoingState = null;
        repaint();
        boolean confirmationFlashActive = state != null && state.awaitingConfirmation();
        if (pressScale == targetPressScale && totalTimeOpacity == targetTotalTimeOpacity
                && slideProgress >= 1.0 && !confirmationFlashActive) animationTimer.stop();
    }

    private boolean containsCircle(int mouseX, int mouseY) {
        int padding = padding();
        int diameter = max(80, Math.min(getWidth(), getHeight()) - padding * 2);
        double dx = mouseX - getWidth() / 2.0;
        double dy = mouseY - getHeight() / 2.0;
        return dx * dx + dy * dy <= diameter * diameter / 4.0;
    }

    private int padding() {
        return compactMode ? 8 : OUTER_PADDING;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        BufferedImage current = renderState(state);
        Graphics2D output = (Graphics2D) graphics.create();
        try {
            output.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            output.translate(centerX, centerY);
            output.scale(pressScale, pressScale);
            output.translate(-centerX, -centerY);

            if (outgoingState != null && slideProgress < 1.0) {
                double eased = 1.0 - Math.pow(1.0 - slideProgress, 3.0);
                int direction = slideDirection;
                BufferedImage old = renderState(outgoingState);
                output.drawImage(old, (int) (-direction * eased * getWidth()), 0, null);
                output.drawImage(current, (int) (direction * (1.0 - eased) * getWidth()), 0, null);
            } else {
                output.drawImage(current, 0, 0, null);
            }
        } finally {
            output.dispose();
        }
    }

    private BufferedImage renderState(TimerState timerState) {
        BufferedImage image = new BufferedImage(max(1, getWidth()), max(1, getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int padding = padding();
            int diameter = Math.min(getWidth(), getHeight()) - padding * 2;
            diameter = max(80, diameter);
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;

            Color foreground = UIManager.getColor("Label.foreground");
            if (foreground == null) {
                foreground = Color.DARK_GRAY;
            }
            Color trackColor = new Color(
                    foreground.getRed(),
                    foreground.getGreen(),
                    foreground.getBlue(),
                    38
            );

            g2.setStroke(new BasicStroke(compactMode ? COMPACT_STROKE_WIDTH : STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(trackColor);
            g2.drawArc(x, y, diameter, diameter, 90, -360);

            Color sectionColor = timerState != null && timerState.section() != null
                    ? ColorUtil.fromHex(timerState.section().getColorHex())
                    : new Color(79, 142, 247);
            boolean waiting = timerState != null && timerState.awaitingConfirmation();
            boolean paused = timerState != null && !timerState.running()
                    && !timerState.complete() && !waiting;
            if (paused) {
                foreground = Theme.MUTED_TEXT;
                sectionColor = Theme.MUTED_TEXT;
            } else if (waiting) {
                boolean colorPhase = System.nanoTime() % FLASH_PERIOD < FLASH_PHASE_DURATION;
                if (!colorPhase) {
                    foreground = Theme.MUTED_TEXT;
                    sectionColor = Theme.MUTED_TEXT;
                }
            }
            double progress = timerState == null ? 0 : timerState.progress();

            if (progress > 0) {
                double arcAngle = max(0.5, progress * 360.0);
                g2.setColor(sectionColor);
                g2.draw(new Arc2D.Double(
                        x, y, diameter, diameter,
                        90.0, -arcAngle,
                        Arc2D.OPEN
                ));
            }

            drawCenterText(g2, x, y, diameter, foreground, sectionColor, timerState);
        } finally {
            g2.dispose();
            graphics.dispose();
        }
        return image;
    }

    private void drawCenterText(
            Graphics2D g2,
            int circleX,
            int circleY,
            int diameter,
            Color foreground,
            Color sectionColor,
            TimerState state
    ) {
        int centerX = circleX + diameter / 2;
        int centerY = circleY + diameter / 2;

        if (state == null || state.profile() == null || state.section() == null) {
            drawCentered(g2, "No timer profiles", centerX, centerY - 8,
                    getFont().deriveFont(Font.BOLD, max(18f, diameter * 0.065f)), foreground);
            drawCentered(g2, "Create a new timer below", centerX, centerY + 26,
                    getFont().deriveFont(Font.PLAIN, max(13f, diameter * 0.04f)), foreground);
            return;
        }

        String sectionName = state.complete() ? "Complete" : state.section().getName();
        String timeText = formatTime(state.remainingMillis());
        String totalTime = formatTime(state.totalRemainingMillis());
        String totalTimeText = state.started() ? "−" + totalTime : "Total: " + totalTime;
        String endTimeText = "Ends at " + LocalDateTime.now()
                .plusNanos(state.totalRemainingMillis() * 1_000_000L)
                .format(END_TIME_FORMAT);

        Font sectionFont = fitFont(g2, sectionName, diameter * 0.67,
                getFont().deriveFont(compactMode ? Font.BOLD : Font.PLAIN, max(compactMode ? 15f : 20f, diameter * 0.075f)));
        Font timeFont = fitFont(g2, timeText, diameter * 0.70,
                getFont().deriveFont(Font.BOLD, max(38f, diameter * 0.16f)));
        Font smallFont = getFont().deriveFont(Font.PLAIN, max(13f, diameter * 0.04f));

        if (compactMode) {
            drawCentered(g2, sectionName, centerX, centerY - 24, sectionFont, sectionColor);
            drawCentered(g2, timeText, centerX, centerY + 14, timeFont, foreground);
            String miniTimeText = state.started() ? endTimeText : totalTimeText;
            drawCenteredWithOpacity(g2, miniTimeText, centerX, centerY + 36,
                    smallFont, foreground, (float) totalTimeOpacity);
        } else {
            drawCentered(g2, sectionName, centerX, centerY - 68, sectionFont, sectionColor);
            drawCentered(g2, timeText, centerX, centerY + 10, timeFont, foreground);
            drawCentered(g2, totalTimeText, centerX, centerY + 58, smallFont, foreground);
            drawCentered(g2, endTimeText, centerX, centerY + 82, smallFont, foreground);
        }
    }

    private Font fitFont(Graphics2D g2, String text, double maximumWidth, Font startingFont) {
        Font font = startingFont;
        while (font.getSize2D() > 12f && g2.getFontMetrics(font).stringWidth(text) > maximumWidth) {
            font = font.deriveFont(font.getSize2D() - 1f);
        }
        return font;
    }

    private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY, Font font, Color color) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics metrics = g2.getFontMetrics(font);
        int x = centerX - metrics.stringWidth(text) / 2;
        g2.drawString(text, x, baselineY);
    }

    private void drawCenteredWithOpacity(Graphics2D g2, String text, int centerX,
                                         int baselineY, Font font, Color color, float opacity) {
        if (opacity <= 0f) return;
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, opacity)));
        drawCentered(g2, text, centerX, baselineY, font, color);
        g2.setComposite(oldComposite);
    }

    private String formatTime(long remainingMillis) {
        long totalSeconds = (remainingMillis + 999L) / 1000L;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
