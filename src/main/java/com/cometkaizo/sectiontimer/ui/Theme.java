package com.cometkaizo.sectiontimer.ui;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import java.awt.Color;

public final class Theme {
    public static final Color BACKGROUND = new Color(18, 20, 25);
    public static final Color SURFACE = new Color(30, 33, 41);
    public static final Color SURFACE_HOVER = new Color(43, 47, 58);
    public static final Color SURFACE_PRESSED = new Color(55, 60, 73);
    public static final Color TEXT = new Color(238, 240, 246);
    public static final Color MUTED_TEXT = new Color(151, 157, 173);
    public static final Color ACCENT = new Color(112, 151, 255);

    private Theme() { }

    public static void install() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(63, 68, 82)),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        UIManager.put("Spinner.background", SURFACE);
        UIManager.put("Spinner.foreground", TEXT);
        UIManager.put("Viewport.background", BACKGROUND);
        UIManager.put("ToolTip.background", SURFACE_HOVER);
        UIManager.put("ToolTip.foreground", TEXT);
    }
}
