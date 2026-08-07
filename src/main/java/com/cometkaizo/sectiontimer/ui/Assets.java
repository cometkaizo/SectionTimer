package com.cometkaizo.sectiontimer.ui;


import com.cometkaizo.sectiontimer.Main;
import com.cometkaizo.sectiontimer.util.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Assets {
    private static final Map<String, Image> TEXTURES = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Font> FONTS = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Sound> SOUNDS = Collections.synchronizedMap(new HashMap<>());

    /// Returns the texture at the given path in the assets folder
    public static Image texture(String path) {
        return TEXTURES.computeIfAbsent("/assets/texture/" + path + ".png", p -> {
            var image = ImageUtils.readImageOrNull(p);
            if (image == null) {
                Main.err("no texture at " + p);
                return ImageUtils.readImage("/assets/texture/unknown.png");
            } else return image;
        });
    }

    /// Returns the default font
    public static Font font() {
        return font("BoldPixels");
    }
    /// Returns the default font with the given size
    public static Font font(int size) {
        return font("BoldPixels", size);
    }
    /// Returns the font at the given path, with the given size
    public static Font font(String path, int size) {
        return font(path).deriveFont(Font.PLAIN, size);
    }
    /// Returns the font at the given path, with the default size
    public static Font font(String path) {
        return FONTS.computeIfAbsent("/assets/gui/font/" + path + ".ttf", p -> {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, Main.getResource(p));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /// Returns the sound at the given path in the assets folder
    public static Sound sound(String path) {
        return SOUNDS.computeIfAbsent("/assets/sound/" + path + ".wav", p -> {
            try (var in = new BufferedInputStream(Main.getResource(p))) {
                return new Sound(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    /// Returns sound at the given path in the assets folder with the given change in pitch
    public static Sound sound(String path, float deltaPitchInSemitones) {
        String fullPath = "/assets/sound/" + path + ".wav";
        return SOUNDS.computeIfAbsent(fullPath + " with delta pitch: " + deltaPitchInSemitones, ignored -> {
            try (var in = new BufferedInputStream(Main.getResource(fullPath))) {
                return new Sound(in, deltaPitchInSemitones);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /// makes a copy of an image
    public static BufferedImage copy(Image image) {
        var copy = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );

        var g = copy.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return copy;
    }
}
