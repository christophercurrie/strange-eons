package org.strangeeons.core.graphics;

import ca.cgjennings.graphics.shapes.AbstractVectorImage;
import ca.cgjennings.graphics.shapes.VectorImage;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * Draws the stand-in shown when an image cannot be loaded: a crossed-out
 * square.
 */
public final class MissingImages {

    private MissingImages() {
    }

    /**
     * Returns a vector stand-in drawn with the given colors.
     *
     * @param fill the square's fill
     * @param cross the color of the cross
     * @return a 17 by 17 unit vector image
     */
    public static VectorImage create(Color fill, Color cross) {
        return new AbstractVectorImage() {
            {
                tx = ty = 1;
                iw = ih = 17;
            }

            @Override
            protected void render(Graphics2D g) {
                Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Stroke os = g.getStroke();
                Paint op = g.getPaint();
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setPaint(fill);
                g.fillRect(0, 0, 15, 15);
                g.setPaint(Color.BLACK);
                g.drawRect(0, 0, 15, 15);
                g.setPaint(cross);
                g.drawLine(3, 3, 12, 12);
                g.drawLine(3, 12, 12, 3);
                g.setPaint(op);
                g.setStroke(os);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
            }
        };
    }

    /**
     * Returns a vector stand-in in the light theme's pink and red.
     *
     * @return the default stand-in
     */
    public static VectorImage createDefault() {
        return create(new Color(0xf8bbd0), new Color(0xb71c1c));
    }
}
