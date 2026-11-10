package org.strangeeons.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.cgjennings.layout.MarkupRenderer;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class MarkupImagesTest {

    private static int inked(BufferedImage im, int x0, int x1) {
        int count = 0;
        for (int y = 0; y < im.getHeight(); ++y) {
            for (int x = x0; x < x1; ++x) {
                if ((im.getRGB(x, y) >>> 24) != 0) {
                    ++count;
                }
            }
        }
        return count;
    }

    @Test
    void rendersMarkupOntoATransparentImage() {
        BufferedImage im = MarkupImages.render("<b>Hello</b> world", 300, 60, 150);
        assertEquals(300, im.getWidth());
        assertEquals(60, im.getHeight());
        assertTrue(inked(im, 0, 300) > 0);
        assertEquals(0, im.getRGB(299, 59) >>> 24);
    }

    @Test
    void renderingIsRepeatable() {
        BufferedImage a = MarkupImages.render("Repeat <i>me</i>", 200, 40, 150);
        BufferedImage b = MarkupImages.render("Repeat <i>me</i>", 200, 40, 150);
        for (int y = 0; y < a.getHeight(); ++y) {
            for (int x = 0; x < a.getWidth(); ++x) {
                assertEquals(a.getRGB(x, y), b.getRGB(x, y));
            }
        }
    }

    @Test
    void drawsOnlyInTheRegion() {
        BufferedImage im = MarkupImages.render(new MarkupRenderer(150), "Left side text", 400, 60,
                new Rectangle2D.Double(0, 0, 200, 60));
        assertTrue(inked(im, 0, 200) > 0);
        assertEquals(0, inked(im, 210, 400));
    }

    @Test
    void usesTheGivenRenderer() {
        MarkupRenderer renderer = new MarkupRenderer(150);
        renderer.setReplacementForTag("name", "Replaced");
        BufferedImage tagged = MarkupImages.render(renderer, "<name>", 300, 60);
        BufferedImage plain = MarkupImages.render("Replaced", 300, 60, 150);
        assertEquals(inked(plain, 0, 300), inked(tagged, 0, 300));
        assertThrows(NullPointerException.class, () -> MarkupImages.render(null, "x", 10, 10));
        assertThrows(NullPointerException.class,
                () -> MarkupImages.render(new MarkupRenderer(), "x", 10, 10, null));
    }
}
