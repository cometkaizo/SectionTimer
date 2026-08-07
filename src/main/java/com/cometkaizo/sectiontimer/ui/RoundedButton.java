package com.cometkaizo.sectiontimer.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RoundedButton extends JButton {
    public RoundedButton(String text) {
        this(text, 9, 16, 9, 16);
    }
    public RoundedButton(String text, int borderTop, int borderLeft, int borderBottom, int borderRight) {
        super(text);
        setForeground(Theme.TEXT);
        setBorder(new EmptyBorder(borderTop, borderLeft, borderBottom, borderRight));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Assets.sound("button_down").play();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Assets.sound("button_up").play();
            }
        });
    }

    private static void paintBackground(AbstractButton button, Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = button.getModel().isPressed() ? Theme.SURFACE_PRESSED
                : button.getModel().isRollover() ? Theme.SURFACE_HOVER : Theme.SURFACE;
        if (!button.isEnabled()) color = new Color(27, 29, 35);
        g2.setColor(color);
        g2.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), 16, 16);
        g2.dispose();
    }

    @Override protected void paintComponent(Graphics graphics) {
        paintBackground(this, graphics);
        super.paintComponent(graphics);
    }
}
