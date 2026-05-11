package ca.cgjennings.graphics.spi;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.strangeeons.plugins.graphics.BlendMode;
import org.strangeeons.plugins.graphics.Font;
import org.strangeeons.plugins.graphics.FontMetrics;
import org.strangeeons.plugins.graphics.Paint;
import org.strangeeons.plugins.graphics.Region;
import org.strangeeons.plugins.graphics.Shape;
import org.strangeeons.plugins.graphics.Stroke;
import org.strangeeons.plugins.graphics.Transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwtGraphicsTest {

    private BufferedImage canvas;
    private Graphics2D g2;
    private AwtGraphics g;

    @BeforeEach
    void setUp() {
        canvas = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        g2 = canvas.createGraphics();
        g = new AwtGraphics(g2);
    }

    @Test
    void fillRectPaintsPixels() {
        g.fillRect(10, 10, 20, 20, Paint.color("FF0000"));
        g2.dispose();
        assertEquals(0xFFFF0000, canvas.getRGB(15, 15));
        assertEquals(0x00000000, canvas.getRGB(40, 40));
    }

    @Test
    void fillRoundRectPaintsCorner() {
        g.fillRoundRect(10, 10, 40, 40, 10, 10, Paint.color("00FF00"));
        g2.dispose();
        // Center is inside the filled region
        assertEquals(0xFF00FF00, canvas.getRGB(30, 30));
        // The extreme rounded corner pixel is outside the fill
        assertEquals(0x00000000, canvas.getRGB(10, 10));
    }

    @Test
    void drawRectOutlinesShape() {
        g.drawRect(10, 10, 20, 20, Stroke.solid(1, Paint.color("0000FF")));
        g2.dispose();
        // Pixel on the outline edge
        assertNotEquals(0, canvas.getRGB(10, 15) & 0xFF000000);
        // Pixel inside the rectangle (unfilled) stays transparent
        assertEquals(0x00000000, canvas.getRGB(20, 20));
    }

    @Test
    void drawLinePaintsBetweenPoints() {
        g.drawLine(10, 50, 90, 50, Stroke.solid(1, Paint.color("FF00FF")));
        g2.dispose();
        // Midpoint should be on the line
        assertNotEquals(0, canvas.getRGB(50, 50) & 0xFF000000);
    }

    @Test
    void drawImageAtPositionCopiesPixels() {
        BufferedImage source = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                source.setRGB(x, y, 0xFF112233);
            }
        }
        g.drawImage(new AwtImageHandle(source), 20, 20);
        g2.dispose();
        assertEquals(0xFF112233, canvas.getRGB(25, 25));
        assertEquals(0x00000000, canvas.getRGB(10, 10));
    }

    @Test
    void drawImageInRegionScales() {
        BufferedImage source = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFAABBCC);
        g.drawImage(new AwtImageHandle(source), new Region(10, 10, 40, 40));
        g2.dispose();
        assertEquals(0xFFAABBCC, canvas.getRGB(25, 25));
    }

    @Test
    void fillUsesAwtShape() {
        AwtShape circle = new AwtShape(new Ellipse2D.Double(10, 10, 40, 40));
        g.fill(circle, Paint.color("FFAA00"));
        g2.dispose();
        assertEquals(0xFFFFAA00, canvas.getRGB(30, 30));
        assertEquals(0x00000000, canvas.getRGB(10, 10));
    }

    @Test
    void drawUsesAwtShape() {
        AwtShape rect = new AwtShape(new Rectangle2D.Double(20, 20, 30, 30));
        g.draw(rect, Stroke.solid(2, Paint.color("808080")));
        g2.dispose();
        assertNotEquals(0, canvas.getRGB(20, 35) & 0xFF000000);
    }

    @Test
    void withTransformRestoresAfterBlock() {
        AffineTransform before = g2.getTransform();
        g.withTransform(Transform.translate(50, 50),
                inner -> inner.fillRect(0, 0, 10, 10, Paint.color("00FFFF")));
        AffineTransform after = g2.getTransform();
        g2.dispose();
        assertEquals(before, after);
        // The fill was translated to (50, 50) inside the lambda
        assertEquals(0xFF00FFFF, canvas.getRGB(55, 55));
        // The original origin is untouched
        assertEquals(0x00000000, canvas.getRGB(5, 5));
    }

    @Test
    void withClipRestoresAfterBlock() {
        java.awt.Shape before = g2.getClip();
        g.withClip(new AwtShape(new Rectangle2D.Double(0, 0, 20, 20)),
                inner -> inner.fillRect(0, 0, 100, 100, Paint.color("FF0000")));
        java.awt.Shape after = g2.getClip();
        g2.dispose();
        // Clip was active only inside the lambda
        assertEquals(before, after);
        // Inside clip region: painted
        assertEquals(0xFFFF0000, canvas.getRGB(10, 10));
        // Outside clip region: untouched
        assertEquals(0x00000000, canvas.getRGB(50, 50));
    }

    @Test
    void withAlphaBlendsWithExistingPixels() {
        g.fillRect(0, 0, 100, 100, Paint.color("FF000000"));   // opaque black
        g.withAlpha(0.5,
                inner -> inner.fillRect(0, 0, 100, 100, Paint.color("FFFFFFFF")));
        g2.dispose();
        int rgb = canvas.getRGB(50, 50) & 0xFFFFFF;
        // 50% blend of black and white should be roughly grey
        int r = (rgb >> 16) & 0xFF;
        assertTrue(r > 100 && r < 160, "expected mid-grey, got #" + Integer.toHexString(rgb));
    }

    @Test
    void withBlendModeClearErasesPixels() {
        g.fillRect(0, 0, 50, 50, Paint.color("FFFF0000"));   // opaque red
        g.withBlendMode(BlendMode.CLEAR,
                inner -> inner.fillRect(10, 10, 20, 20, Paint.color("FFFFFFFF")));
        g2.dispose();
        // Pixel inside the cleared rectangle is transparent
        assertEquals(0x00000000, canvas.getRGB(20, 20));
        // Pixel outside is still red
        assertEquals(0xFFFF0000, canvas.getRGB(40, 40));
    }

    @Test
    void measureTextReturnsNonZeroMetrics() {
        FontMetrics m = g.measureText("hello", Font.of("Dialog", 12));
        g2.dispose();
        assertTrue(m.getWidth() > 0, "width was " + m.getWidth());
        assertTrue(m.getHeight() > 0, "height was " + m.getHeight());
        assertTrue(m.getAscent() > 0, "ascent was " + m.getAscent());
    }

    @Test
    void drawStringChangesPixels() {
        g.drawString("X", 50, 50, new Font("Dialog", 24, Font.Style.BOLD), Paint.color("000000"));
        g2.dispose();
        // Some pixel near the baseline should be painted
        boolean foundInk = false;
        for (int dx = -10; dx < 10 && !foundInk; dx++) {
            for (int dy = -20; dy < 5 && !foundInk; dy++) {
                if ((canvas.getRGB(50 + dx, 50 + dy) & 0xFF000000) != 0) {
                    foundInk = true;
                }
            }
        }
        assertTrue(foundInk, "expected glyph pixels near (50,50)");
    }

    @Test
    void unwrapForeignImageHandleThrows() {
        var foreign = new org.strangeeons.plugins.graphics.ImageHandle() {
            @Override public int getWidth()    { return 1; }
            @Override public int getHeight()   { return 1; }
            @Override public String getSource() { return null; }
        };
        assertThrows(IllegalArgumentException.class,
                () -> g.drawImage(foreign, 0, 0));
    }

    @Test
    void unwrapForeignShapeThrows() {
        Shape foreign = new Shape() { };
        assertThrows(IllegalArgumentException.class,
                () -> g.fill(foreign, Paint.color("000000")));
    }

    @Test
    void awtImageHandleReportsDimensions() {
        BufferedImage src = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        AwtImageHandle handle = new AwtImageHandle(src);
        assertEquals(64, handle.getWidth());
        assertEquals(32, handle.getHeight());
        assertSame(src, handle.image());
    }

    @Test
    void awtImageHandleCarriesSource() {
        BufferedImage src = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        AwtImageHandle handle = new AwtImageHandle(src, "test/portrait.png");
        assertEquals("test/portrait.png", handle.getSource());

        AwtImageHandle anonymous = new AwtImageHandle(src);
        assertEquals(null, anonymous.getSource());
    }
}
