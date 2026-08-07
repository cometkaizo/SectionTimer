package com.cometkaizo.sectiontimer.ui;

import java.awt.Color;

final class ColorUtil {
    private ColorUtil() {
    }

    static Color fromHex(String hex) {
        try {
            return Color.decode(hex);
        } catch (NumberFormatException | NullPointerException exception) {
            return new Color(79, 142, 247);
        }
    }

    static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    static Color readableTextColor(Color background) {
        double luminance = 0.2126 * background.getRed()
                + 0.7152 * background.getGreen()
                + 0.0722 * background.getBlue();
        return luminance < 145 ? Color.WHITE : Color.BLACK;
    }
}
