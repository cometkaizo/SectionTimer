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
        return TEXTURES.computeIfAbsent("/assets/" + path + ".png", p -> {
            var image = ImageUtils.readImageOrNull(p);
            if (image == null) {
                Main.err("no texture at " + p);
                return ImageUtils.readImage("/assets/unknown.png");
            } else return image;
        });
    }
    /// Returns the texture at the given path in the assets folder, with an outline applied dynamically
    public static Image textureOutlined(String path) {
        String key = "/assets/" + path + " OUTLINE";
        if (TEXTURES.containsKey(key)) return TEXTURES.get(key);

        var origImage = (BufferedImage) texture(path);
        var firstImage = copy(origImage);
        apply1PixelOutline(firstImage, origImage, Color.WHITE);
        var outlinedImage = copy(firstImage);
        apply1PixelOutline(outlinedImage, firstImage, Color.BLACK);

        TEXTURES.put(key, outlinedImage);
        return outlinedImage;
    }
    /// Applies a one pixel outline with the given color and stores it into the output parameter image
    private static void apply1PixelOutline(BufferedImage image, BufferedImage origImage, Color color) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        int outlineColor = color.getRGB();
        for (int x = 0; x < w; x ++) {
            for (int y = 0; y < h; y ++) {
                if (isTranslucent(origImage, x, y)) {
                    if (!isTranslucent(origImage, x-1, y) ||
                            !isTranslucent(origImage, x, y-1) ||
                            !isTranslucent(origImage, x+1, y) ||
                            !isTranslucent(origImage, x, y+1)) {
                        image.setRGB(x, y, outlineColor);
                    }
                }
            }
        }
    }
    /// Returns false if the pixel at the given position in the image is opaque, and true otherwise
    private static boolean isTranslucent(BufferedImage img, int x, int y) {
        if (x < 0 || x >= img.getWidth() || y < 0 || y >= img.getHeight()) return true;
        int pixel = img.getRGB(x, y);
        // alpha is stored in the 8 bits at the far left
        int alpha = (pixel >> 24) & 0xff;
        return alpha < 255;
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
        return SOUNDS.computeIfAbsent(fullPath + " with delta pitch: " + deltaPitchInSemitones, _ -> {
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
