package ca.cgjennings.graphics.spi;

import org.strangeeons.plugins.graphics.FontMetrics;

/**
 * Immutable snapshot of {@link FontMetrics} backed by a measurement
 * taken from {@link java.awt.FontMetrics}.
 */
record AwtFontMetrics(double width, double height,
                      double ascent, double descent,
                      double leading) implements FontMetrics {

    @Override public double getWidth()   { return width; }
    @Override public double getHeight()  { return height; }
    @Override public double getAscent()  { return ascent; }
    @Override public double getDescent() { return descent; }
    @Override public double getLeading() { return leading; }
}
