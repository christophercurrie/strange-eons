package org.strangeeons.plugins.graphics;

/**
 * Measured dimensions for a text run, returned by
 * {@link Graphics#measureText(String, Font)}. All measurements are
 * in template-resolution pixels.
 *
 * <p>Replaces both {@code java.awt.FontMetrics} (which exposes a
 * mutable AWT type) and {@code java.awt.font.FontRenderContext}
 * (which exposes AWT-specific rendering-context semantics). Plug-ins
 * that need to lay out text by hand call {@code measureText} on the
 * current {@link Graphics}.
 *
 * @since 3.6
 */
public interface FontMetrics {

    /** Returns the rendered width of the measured text. */
    double getWidth();

    /** Returns the total height (ascent + descent + leading). */
    double getHeight();

    /** Returns the distance from the baseline to the top of the tallest glyph. */
    double getAscent();

    /** Returns the distance from the baseline to the bottom of the lowest glyph. */
    double getDescent();

    /** Returns the recommended additional spacing between consecutive lines. */
    double getLeading();
}
