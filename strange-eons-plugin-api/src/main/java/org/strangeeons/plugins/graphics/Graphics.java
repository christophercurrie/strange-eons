package org.strangeeons.plugins.graphics;

import java.util.function.Consumer;

/**
 * The painter surface passed to component handlers during a paint
 * cycle. Shape primitives take {@link Paint} or {@link Stroke} as
 * per-call parameters; transforms, clips, alpha, and blend modes
 * apply via lambda-scoped {@code withX} blocks that automatically
 * restore state on exit.
 *
 * <p>All coordinates are in template-resolution pixels expressed as
 * {@code double} for sub-pixel precision.
 *
 * @since 3.6
 */
public interface Graphics {

    // ---------------------------------------------------------------
    // Shape primitives — paint or stroke per call
    // ---------------------------------------------------------------

    /** Fills a rectangle with the given paint. */
    void fillRect(double x, double y, double width, double height, Paint paint);

    /** Outlines a rectangle with the given stroke. */
    void drawRect(double x, double y, double width, double height, Stroke stroke);

    /** Fills a rounded rectangle with the given paint. */
    void fillRoundRect(double x, double y, double width, double height,
                       double arcWidth, double arcHeight, Paint paint);

    /** Outlines a rounded rectangle with the given stroke. */
    void drawRoundRect(double x, double y, double width, double height,
                       double arcWidth, double arcHeight, Stroke stroke);

    /** Draws a line between two points with the given stroke. */
    void drawLine(double x1, double y1, double x2, double y2, Stroke stroke);

    /** Fills an arbitrary shape with the given paint. */
    void fill(Shape shape, Paint paint);

    /** Outlines an arbitrary shape with the given stroke. */
    void draw(Shape shape, Stroke stroke);

    // ---------------------------------------------------------------
    // Image
    // ---------------------------------------------------------------

    /**
     * Draws an image at the given top-left position, at its natural
     * size.
     */
    void drawImage(ImageHandle image, double x, double y);

    /**
     * Draws an image scaled into the given region.
     */
    void drawImage(ImageHandle image, Region region);

    // ---------------------------------------------------------------
    // Text — font and paint per call
    // ---------------------------------------------------------------

    /**
     * Draws a single line of text at the given baseline position.
     */
    void drawString(String text, double x, double y, Font font, Paint paint);

    /**
     * Measures a text run in the given font. The returned metrics
     * describe ascent, descent, leading, and the rendered width.
     */
    FontMetrics measureText(String text, Font font);

    // ---------------------------------------------------------------
    // Lambda-scoped global state
    // ---------------------------------------------------------------

    /**
     * Applies the given transform to all draws performed inside the
     * block, then restores the previous transform on exit. Transforms
     * compose with any enclosing transform.
     */
    void withTransform(Transform transform, Consumer<Graphics> block);

    /**
     * Restricts all draws performed inside the block to the given
     * clip region, then restores the previous clip on exit. Clip
     * intersects with any enclosing clip.
     */
    void withClip(Shape clip, Consumer<Graphics> block);

    /**
     * Applies an alpha multiplier in {@code [0.0, 1.0]} to all draws
     * performed inside the block, then restores the previous alpha
     * on exit.
     */
    void withAlpha(double alpha, Consumer<Graphics> block);

    /**
     * Applies the given blend mode to all draws performed inside the
     * block, then restores the previous blend mode on exit.
     */
    void withBlendMode(BlendMode mode, Consumer<Graphics> block);
}
