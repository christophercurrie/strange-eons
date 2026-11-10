package org.strangeeons.testkit;

import ca.cgjennings.layout.MarkupRenderer;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;

/**
 * Renders markup to images, using the rendering hints Strange Eons uses when
 * exporting or printing a sheet. Text measurements depend on the installed
 * fonts, so keep separate masters for each platform tests run on.
 */
public final class MarkupImages {

    private static final Map<RenderingHints.Key, Object> HINTS = Map.of(
            RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY,
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON,
            RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY,
            RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE,
            RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON,
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC,
            RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY,
            RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE,
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    private MarkupImages() {
    }

    /**
     * Renders markup with a new renderer's default styles.
     *
     * @param markup the markup to render
     * @param width the image width, in pixels
     * @param height the image height, in pixels
     * @param ppi the resolution, in pixels per inch, that point sizes use
     * @return a transparent image with the markup drawn over the whole area
     */
    public static BufferedImage render(String markup, int width, int height, double ppi) {
        return render(new MarkupRenderer(ppi), markup, width, height);
    }

    /**
     * Renders markup with a configured renderer, such as one a plug-in sets
     * up with its own styles and replacement tags.
     *
     * @param renderer the renderer to use; its markup text is replaced
     * @param markup the markup to render
     * @param width the image width, in pixels
     * @param height the image height, in pixels
     * @return a transparent image with the markup drawn over the whole area
     */
    public static BufferedImage render(MarkupRenderer renderer, String markup, int width, int height) {
        return render(renderer, markup, width, height, new Rectangle2D.Double(0, 0, width, height));
    }

    /**
     * Renders markup into one region of an image.
     *
     * @param renderer the renderer to use; its markup text is replaced
     * @param markup the markup to render
     * @param width the image width, in pixels
     * @param height the image height, in pixels
     * @param region the area the text is laid out in
     * @return a transparent image with the markup drawn in the region
     */
    public static BufferedImage render(MarkupRenderer renderer, String markup, int width, int height,
            Rectangle2D region) {
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(region, "region");
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHints(HINTS);
            renderer.setMarkupText(markup);
            renderer.draw(g, region);
        } finally {
            g.dispose();
        }
        return image;
    }
}
