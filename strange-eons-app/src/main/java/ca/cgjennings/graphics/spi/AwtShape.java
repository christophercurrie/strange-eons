package ca.cgjennings.graphics.spi;

import java.util.Objects;
import org.strangeeons.plugins.graphics.Shape;

/**
 * A {@link Shape} backed by a concrete {@link java.awt.Shape}. The
 * framework uses this to surface AWT-derived shapes (template clip
 * regions, computed paths, etc.) through the SPI;
 * {@link AwtGraphics} unwraps the underlying AWT shape when filling,
 * drawing, or clipping.
 */
public final class AwtShape implements Shape {

    private final java.awt.Shape shape;

    public AwtShape(java.awt.Shape shape) {
        this.shape = Objects.requireNonNull(shape, "shape");
    }

    /** Returns the underlying AWT shape. Package-private use only. */
    java.awt.Shape shape() {
        return shape;
    }
}
