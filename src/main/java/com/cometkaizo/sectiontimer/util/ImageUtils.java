package com.cometkaizo.sectiontimer.util;

import com.cometkaizo.sectiontimer.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Author: Andy Wang
 * Date Modified: 2025-12-19
 * Description: Useful image-related methods
 */
public class ImageUtils {

    /**
     * Apply a mask (alpha channel) on an image
     * @param image the image to mask
     * @param mask the mask to apply
     * @return The transformed image
     */
    public static BufferedImage applyTransparency(BufferedImage image, Image mask) {
        BufferedImage dest = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dest.createGraphics();
        g2.drawImage(image, 0, 0, null);
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.DST_IN, 1.0F);
        g2.setComposite(ac);
        g2.drawImage(mask, 0, 0, null);
        g2.dispose();
        return dest;
    }

    /// Reads the image in the given file
    public static BufferedImage readImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /// Reads the image from the given resource path (in the resources folder)
    public static BufferedImage readImage(String resourceLoc) {
        try {
            return ImageIO.read(Main.getResource(resourceLoc));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /// Reads the image from the given resource path, or returns null if it does not exist
    public static BufferedImage readImageOrNull(String resourceLoc) {
        try {
            return ImageIO.read(Main.getResource(resourceLoc));
        } catch (Exception e) {
            return null;
        }
    }

}
