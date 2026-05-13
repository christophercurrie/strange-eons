package ca.cgjennings.graphics.spi;

import java.awt.image.BufferedImage;
import java.util.Objects;
import org.strangeeons.plugins.graphics.ImageHandle;

/**
 * An {@link ImageHandle} backed by a concrete {@link BufferedImage}.
 * The framework uses this to surface AWT-decoded bitmaps through the
 * SPI; the underlying image is retrievable via {@link #image()} for
 * code inside this package (notably {@link AwtGraphics}).
 */
public final class AwtImageHandle implements ImageHandle {

    private final BufferedImage image;
    private final String source;

    /** Creates a handle with no source identifier. */
    public AwtImageHandle(BufferedImage image) {
        this(image, null);
    }

    /**
     * Creates a handle backed by the given image with the supplied
     * source identifier. The source is typically a resource key,
     * file path, or URL string; may be {@code null} for derived or
     * synthesised images.
     */
    public AwtImageHandle(BufferedImage image, String source) {
        this.image = Objects.requireNonNull(image, "image");
        this.source = source;
    }

    /**
     * Returns the underlying AWT image. Framework code that needs to
     * bridge legacy AWT-typed APIs into the new SPI surface uses this;
     * plug-ins see only the {@link ImageHandle} interface and so cannot
     * reach the underlying object through the SPI.
     */
    public BufferedImage image() {
        return image;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public String getSource() {
        return source;
    }
}
