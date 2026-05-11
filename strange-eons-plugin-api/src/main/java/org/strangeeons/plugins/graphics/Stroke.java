package org.strangeeons.plugins.graphics;

/**
 * Describes how a line or shape outline is drawn. {@code Stroke} is
 * a sealed hierarchy: plug-ins construct instances via the static
 * factory methods on this interface, and the framework
 * pattern-matches on the nested record types when rendering.
 *
 * @since 3.6
 */
public sealed interface Stroke {

    /** Returns the stroke line width in template-resolution pixels. */
    double getWidth();

    /** Returns the paint used to render the stroke. */
    Paint getPaint();

    /**
     * A solid (non-dashed) stroke with a uniform line width and a
     * fill paint.
     */
    record Solid(double width, Paint paint) implements Stroke {
        @Override public double getWidth() { return width; }
        @Override public Paint getPaint() { return paint; }
    }

    /**
     * Creates a solid stroke with the given line width and paint.
     */
    static Stroke solid(double width, Paint paint) {
        return new Solid(width, paint);
    }
}
