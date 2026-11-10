package org.strangeeons.core.graphics;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.util.Objects;

/**
 * Converts images between pixel formats and computes scales for fitting
 * images to an area.
 */
public final class ImageFormats {

    private ImageFormats() {
    }

    /**
     * Creates a new image the same size as {@code im} which is in either
     * {@code TYPE_INT_RGB} or {@code TYPE_INT_ARGB} format depending on
     * whether
     * {@code im} is opaque.
     *
     * @param im the image to create a new image for
     * @return a new image compatible with {@code im} but guaranteed to use an
     * 8-bit RGB format
     */
    public static BufferedImage createCompatibleIntRGBFormat(BufferedImage im) {
        return createCompatibleIntRGBFormat(im, im.getWidth(), im.getHeight());
    }

    /**
     * Creates a new image with the specified size which is in either
     * {@code TYPE_INT_RGB} or {@code TYPE_INT_ARGB} format depending on
     * whether
     * {@code im} is opaque.
     *
     * @param im the image to create a new image for
     * @return a new image compatible with {@code im} but guaranteed to use an
     * 8-bit RGB format
     */
    public static BufferedImage createCompatibleIntRGBFormat(BufferedImage im, int width, int height) {
        final int type = (im.getTransparency() == BufferedImage.OPAQUE)
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        return new BufferedImage(width, height, type);
    }

    /**
     * Creates a new image the same size as {@code im} which is in
     * {@code TYPE_INT_ARGB} format.
     *
     * @param im the image to create a new image for
     * @return a new image compatible with {@code im} but guaranteed to use
     * 8-bit ARGB format
     */
    public static BufferedImage createCompatibleIntARGBFormat(BufferedImage im) {
        return new BufferedImage(
                im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB
        );
    }

    /**
     * If {@code im} is not in an integer (A)RGB format, returns a new,
     * equivalent image in an integer RGB format. If {@code im} is already in
     * an integer RGB format, returns the original image.
     *
     * @param image the non-null image to be provided in integer RGB format
     * @return the original image, or a copy converted to a suitable format
     */
    public static BufferedImage ensureIntRGBFormat(Image image) {
        if (!(Objects.requireNonNull(image, "image") instanceof BufferedImage)) {
            waitForImage(image);
            BufferedImage im = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = im.createGraphics();
            try {
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
            return im;
        }
        
        BufferedImage im = (BufferedImage) image;
        final int type = im.getType();
        if (type != BufferedImage.TYPE_INT_RGB && type != BufferedImage.TYPE_INT_ARGB && type != BufferedImage.TYPE_INT_ARGB_PRE) {
            BufferedImage dest = createCompatibleIntRGBFormat(im);
            Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(im, 0, 0, null);
            } finally {
                g.dispose();
            }
            im = dest;
        }
        return im;
    }

    /**
     * Returns a version of an image that includes an alpha channel. If the
     * source image has an alpha channel, it is returned. Otherwise, a copy is
     * returned.
     *
     * @param im the image to check
     * @return the original image, or a copy that includes an alpha channel
     */
    public static BufferedImage ensureImageHasAlphaChannel(BufferedImage im) {
        int type = im.getType();
        if (type != BufferedImage.TYPE_INT_ARGB && type != BufferedImage.TYPE_INT_ARGB_PRE) {
            im = ensureImageHasType(im, BufferedImage.TYPE_INT_ARGB);
        }
        return im;
    }

    /**
     * If {@code im} does not use the specified pixel format, an equivalent
     * image in that format is returned. If {@code im} is already in the
     * specified format, it is returned unchanged.
     *
     * @param im the image to be provided in the specified format
     * @param type the {@code BufferedImage.TYPE_*} value for the desired format
     * @return the original image, or a copy converted to the requested format
     */
    public static BufferedImage ensureImageHasType(BufferedImage im, int type) {
        if (im.getType() != type) {
            BufferedImage dest = new BufferedImage(im.getWidth(), im.getHeight(), type);
            Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(im, 0, 0, null);
            } finally {
                g.dispose();
            }
            im = dest;
        }
        return im;
    }

    /**
     * Returns a version of an image that is in an integer (A)RGB format. If the
     * source includes an alpha channel, the returned image will use a
     * premultiplied image type.
     *
     * @param im the image to be provided in the specified format
     * @return the original image, or a copy in an integer (A)RGB format, with
     * premultiplied alpha if appropriate
     */
    public static BufferedImage ensurePremultipliedFormat(BufferedImage im) {
        if (im.getTransparency() != Transparency.OPAQUE) {
            im = ensureImageHasType(im, BufferedImage.TYPE_INT_ARGB_PRE);
        } else {
            im = ensureImageHasType(im, BufferedImage.TYPE_INT_RGB);
        }
        return im;
    }

    /**
     * Waits for an image to load.
     * Generic {@code Image} instances, such as those obtained from the
     * {@code Toolkit} are designed to load in
     * the background. As a result, certain methods may return wrong or
     * temporary values. This method ensures that a target image is fully
     * loaded (or failed to load) before returning.
     * Note that {@code BufferedImage}s never load in the background.
     * If passed a {@code BufferedImage} this method returns immediately.
     * 
     * @param im the image to wait for
     * @return the image that was passed in
     */
    public static Image waitForImage(Image im) {
        // common case, and always "loaded"
        if (im instanceof BufferedImage) {
            return im;
        }
        
        // needed to get a graphics context
        final BufferedImage dest = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        
        // create an image observer to notify us when the image has loaded
        final Object synchObject = new Object();
        final ImageObserver obs = (Image img, int stateFlags, int x, int y, int width, int height) -> {
            if (stateFlags < ImageObserver.ALLBITS) {
                // not done; wait for more status updates
                return true;
            }
            
            // done, wake up the calling thread
            synchronized (synchObject) {
                synchObject.notify();
            }
            return false;
        };
        
        synchronized (synchObject) {
            Graphics2D g = dest.createGraphics();
            try {
                // ensure image starts loading, and check if it has finished
                boolean hasLoaded = g.drawImage(im, 0, 0, obs);

                // if it hasn't finished, sleep until it finishes, at which point
                // the observer we created and passed to drawImage will notify us
                if (!hasLoaded) {
                    for (;;) {
                        try {
                            // release monitor and wait for observer
                            synchObject.wait(0);
                            break;
                        } catch (InterruptedException iex) {
                            // keep waiting
                        }
                    }
                }
            } finally {
                g.dispose();
            }
        }
        
        return im;
    }

    /**
     * Returns a {@link BufferedImage} that is equivalent to a specified
     * {@link Image}. If the image is already a {@code BufferedImage}, it is
     * returned unmodified. Otherwise, a new {@code BufferedImage} will be
     * created and the contents of the image copied into it. If necessary, the
     * method will wait until the image has finished downloading.
     *
     * @param image the non-null image to convert
     * @return the original image, converted into a {@code BufferedImage} if it
     * is not already of that type
     */
    public static BufferedImage imageToBufferedImage(Image image) {
        if (Objects.requireNonNull(image, "image") instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        
        waitForImage(image);
        
        // default to ARGB, but try to detect RGB
        int type = BufferedImage.TYPE_INT_ARGB;
        final int w = image.getWidth(null);
        final int h = image.getHeight(null);
        try {
            PixelGrabber grabber = new PixelGrabber(image, w-1, h-1, 1, 1, false);
            grabber.grabPixels();
            if (!grabber.getColorModel().hasAlpha()) {
                type = BufferedImage.TYPE_INT_RGB;
            }
        } catch (InterruptedException | RuntimeException ex) {
            // stick with default type, which is safe to use
        }

        // convert image
        final BufferedImage im = new BufferedImage(w, h, type);
        Graphics2D g = im.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }

        return im;
    }

    /**
     * Returns the largest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension. The image will either match the ideal size in the other
     * dimension, or else be larger than the other ideal dimension. If the image
     * is opaque, the result is the scaling factor needed to obtain the smallest
     * image with the same aspect ratio that would completely cover the ideal
     * image area.
     *
     * @param idealWidth the width of the area the image must cover
     * @param idealHeight the height of the area the image must cover
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that would ensure that the image would just cover the
     * specified area
     */
    public static double idealCoveringScaleForImage(double idealWidth, double idealHeight, double imageWidth, double imageHeight) {
        double hScale = idealWidth / imageWidth;
        double vScale = idealHeight / imageHeight;

        if (hScale < vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }

    /**
     * Returns the largest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension. The image will either match the ideal size in the other
     * dimension, or else be larger than the other ideal dimension. If the image
     * is opaque, the result is the scaling factor to obtain the smallest image
     * with the same aspect ratio that would completely cover the ideal image
     * area.
     *
     * @param idealWidth the width of the area the image must cover
     * @param idealHeight the height of the area the image must cover
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that would ensure that the image would just cover the
     * specified area
     */
    public static float idealCoveringScaleForImage(float idealWidth, float idealHeight, float imageWidth, float imageHeight) {
        float hScale = idealWidth / imageWidth;
        float vScale = idealHeight / imageHeight;

        if (hScale < vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }

    /**
     * Returns the largest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension. The image will either match the ideal size in the other
     * dimension, or else be larger than the other ideal dimension. If the image
     * is opaque, the result is the scaling factor to obtain the smallest image
     * with the same aspect ratio that would completely cover the ideal image
     * area.
     *
     * @param idealWidth the width of the area the image must cover
     * @param idealHeight the height of the area the image must cover
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that would ensure that the image would just cover the
     * specified area
     */
    public static float idealCoveringScaleForImage(int idealWidth, int idealHeight, int imageWidth, int imageHeight) {
        float hScale = idealWidth / (float) imageWidth;
        float vScale = idealHeight / (float) imageHeight;
        if (hScale < vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }
}
