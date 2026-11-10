package org.strangeeons.core.graphics.awt;

import ca.cgjennings.layout.PageShape;
import ca.cgjennings.layout.TextStyle;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import org.strangeeons.plugins.graphics.Graphics;
import org.strangeeons.plugins.graphics.MarkupRenderer;
import org.strangeeons.plugins.graphics.Region;

/**
 * Presents a {@link ca.cgjennings.layout.MarkupRenderer} as an SPI
 * {@link MarkupRenderer}. The legacy class does not implement the SPI
 * interface itself: overloads taking SPI types would make calls with a
 * {@code null} argument ambiguous for scripts.
 */
public final class AwtMarkupRenderer implements MarkupRenderer {

    private final ca.cgjennings.layout.MarkupRenderer renderer;

    /**
     * Wraps a renderer.
     *
     * @param renderer the renderer that lays out and draws text
     */
    public AwtMarkupRenderer(ca.cgjennings.layout.MarkupRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Returns the wrapped renderer.
     *
     * @return the renderer this delegates to
     */
    public ca.cgjennings.layout.MarkupRenderer renderer() {
        return renderer;
    }

    @Override
    public void setMarkupText(String markup) {
        renderer.setMarkupText(markup);
    }

    @Override
    public String getMarkupText() {
        return renderer.getMarkupText();
    }

    @Override
    public void setDefaultStyle(org.strangeeons.plugins.graphics.TextStyle style) {
        renderer.setDefaultStyle(legacy(style));
    }

    @Override
    public TextStyle getDefaultStyle() {
        return renderer.getDefaultStyle();
    }

    @Override
    public void setStyleForTag(String tag, org.strangeeons.plugins.graphics.TextStyle style) {
        renderer.setStyleForTag(tag, legacy(style));
    }

    @Override
    public void setReplacementForTag(String tag, String replacement) {
        renderer.setReplacementForTag(tag, replacement);
    }

    @Override
    public void setLineTightness(float tightness) {
        renderer.setLineTightness(tightness);
    }

    @Override
    public float getLineTightness() {
        return renderer.getLineTightness();
    }

    @Override
    public void setAlignment(int alignment) {
        renderer.setAlignment(alignment);
    }

    @Override
    public int getAlignment() {
        return renderer.getAlignment();
    }

    @Override
    public void setTextFitting(int mode) {
        renderer.setTextFitting(mode);
    }

    @Override
    public int getTextFitting() {
        return renderer.getTextFitting();
    }

    @Override
    public void setPageShape(org.strangeeons.plugins.graphics.PageShape shape) {
        if (shape != null && !(shape instanceof PageShape)) {
            throw new IllegalArgumentException("unsupported PageShape implementation: " + shape.getClass().getName());
        }
        renderer.setPageShape((PageShape) shape);
    }

    @Override
    public PageShape getPageShape() {
        return renderer.getPageShape();
    }

    @Override
    public double draw(Graphics g, Region region) {
        return renderer.draw(unwrap(g), rectangle(region));
    }

    @Override
    public double drawAsSingleLine(Graphics g, Region region) {
        return renderer.drawAsSingleLine(unwrap(g), rectangle(region));
    }

    @Override
    public double measure(Graphics g, Region region) {
        return renderer.measure(unwrap(g), rectangle(region));
    }

    private static TextStyle legacy(org.strangeeons.plugins.graphics.TextStyle style) {
        if (style == null || style instanceof TextStyle) {
            return (TextStyle) style;
        }
        throw new IllegalArgumentException("unsupported TextStyle implementation: " + style.getClass().getName());
    }

    private static Graphics2D unwrap(Graphics g) {
        if (g instanceof AwtGraphics) {
            return ((AwtGraphics) g).graphics2D();
        }
        throw new IllegalArgumentException("unsupported Graphics implementation: "
                + (g == null ? "null" : g.getClass().getName()));
    }

    private static Rectangle2D rectangle(Region r) {
        return new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }
}
