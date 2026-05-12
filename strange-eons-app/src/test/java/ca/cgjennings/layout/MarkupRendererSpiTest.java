package ca.cgjennings.layout;

import ca.cgjennings.graphics.spi.AwtGraphics;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.strangeeons.plugins.graphics.Graphics;
import org.strangeeons.plugins.graphics.Region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkupRendererSpiTest {

    private BufferedImage canvas;
    private Graphics2D g2;
    private AwtGraphics g;
    private MarkupRenderer mr;

    @BeforeEach
    void setUp() {
        canvas = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        g2 = canvas.createGraphics();
        g = new AwtGraphics(g2);
        mr = new MarkupRenderer();
        TextStyle defaultStyle = new TextStyle(
                TextAttribute.FAMILY, "Dialog",
                TextAttribute.SIZE, 12.0f,
                TextAttribute.FOREGROUND, Color.BLACK);
        mr.setDefaultStyle(defaultStyle);
        mr.setMarkupText("Hello, world.");
    }

    @Test
    void legacyClassImplementsSpiMarkupRenderer() {
        org.strangeeons.plugins.graphics.MarkupRenderer spi = mr;
        assertNotNull(spi);
        assertSame(mr, spi);
    }

    @Test
    void legacyTextStyleImplementsSpiTextStyle() {
        org.strangeeons.plugins.graphics.TextStyle style = new TextStyle();
        assertNotNull(style);
    }

    @Test
    void legacyPageShapeImplementsSpiPageShape() {
        org.strangeeons.plugins.graphics.PageShape shape = PageShape.RECTANGLE_SHAPE;
        assertNotNull(shape);
    }

    @Test
    void setMarkupTextRoundTrips() {
        mr.setMarkupText("<b>bold</b> text");
        assertEquals("<b>bold</b> text", mr.getMarkupText());
    }

    @Test
    void lineTightnessRoundTrips() {
        mr.setLineTightness(0.85f);
        assertEquals(0.85f, mr.getLineTightness(), 1e-6);
    }

    @Test
    void alignmentRoundTrips() {
        mr.setAlignment(org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_CENTER
                | org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_TOP);
        assertEquals(
                org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_CENTER
                        | org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_TOP,
                mr.getAlignment());
    }

    @Test
    void textFittingRoundTrips() {
        mr.setTextFitting(org.strangeeons.plugins.graphics.MarkupRenderer.FIT_TIGHTEN_LINE_SPACING);
        assertEquals(
                org.strangeeons.plugins.graphics.MarkupRenderer.FIT_TIGHTEN_LINE_SPACING,
                mr.getTextFitting());
    }

    @Test
    void layoutConstantsMatchLegacyValues() {
        assertEquals(MarkupRenderer.LAYOUT_LEFT,
                org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_LEFT);
        assertEquals(MarkupRenderer.LAYOUT_CENTER,
                org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_CENTER);
        assertEquals(MarkupRenderer.LAYOUT_JUSTIFY,
                org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_JUSTIFY);
        assertEquals(MarkupRenderer.FIT_BOTH,
                org.strangeeons.plugins.graphics.MarkupRenderer.FIT_BOTH);
    }

    @Test
    void drawViaSpiPaintsPixels() {
        mr.setMarkupText("X");
        mr.setAlignment(org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_LEFT
                | org.strangeeons.plugins.graphics.MarkupRenderer.LAYOUT_TOP);
        TextStyle big = new TextStyle(
                TextAttribute.FAMILY, "Dialog",
                TextAttribute.SIZE, 48.0f,
                TextAttribute.FOREGROUND, Color.BLACK);
        mr.setDefaultStyle(big);

        Graphics gWide = g;
        gWide.fillRect(0, 0, 200, 200,
                org.strangeeons.plugins.graphics.Paint.color("FFFFFFFF"));   // white background
        mr.draw(gWide, new Region(10, 10, 180, 180));
        g2.dispose();

        // Find at least one non-white pixel (the glyph) in the upper-left region.
        boolean foundInk = false;
        for (int y = 10; y < 80 && !foundInk; y++) {
            for (int x = 10; x < 80 && !foundInk; x++) {
                if ((canvas.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    foundInk = true;
                }
            }
        }
        assertTrue(foundInk, "expected glyph pixels in the upper-left region");
    }

    @Test
    void measureViaSpiReturnsNonZeroHeight() {
        mr.setMarkupText("hello world");
        double height = mr.measure(g, new Region(0, 0, 100, 100));
        g2.dispose();
        assertTrue(height > 0.0, "measured height was " + height);
    }

    @Test
    void drawAsSingleLineViaSpiReturnsNonZeroWidth() {
        mr.setMarkupText("hello");
        double width = mr.drawAsSingleLine(g, new Region(0, 0, 200, 50));
        g2.dispose();
        assertTrue(width > 0.0, "single-line width was " + width);
    }

    @Test
    void setDefaultStyleAcceptsSpiTextStyle() {
        org.strangeeons.plugins.graphics.TextStyle spiStyle = new TextStyle(
                TextAttribute.FAMILY, "SansSerif",
                TextAttribute.SIZE, 18.0f);
        mr.setDefaultStyle(spiStyle);
        // Round-trip via the getter — covariant return is the legacy TextStyle
        assertSame(spiStyle, mr.getDefaultStyle());
    }

    @Test
    void setStyleForTagAcceptsSpiTextStyle() {
        org.strangeeons.plugins.graphics.TextStyle spiStyle = new TextStyle(
                TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        // Should not throw
        mr.setStyleForTag("strong", spiStyle);
    }

    @Test
    void setPageShapeAcceptsSpiPageShape() {
        org.strangeeons.plugins.graphics.PageShape spiShape = PageShape.RECTANGLE_SHAPE;
        mr.setPageShape(spiShape);
        assertSame(spiShape, mr.getPageShape());
    }

    @Test
    void foreignGraphicsThrows() {
        Graphics foreign = new ForeignGraphics();
        assertThrows(IllegalArgumentException.class,
                () -> mr.draw(foreign, new Region(0, 0, 10, 10)));
    }

    // Minimal Graphics that is not an AwtGraphics — used to verify the unwrap
    // path rejects unknown implementations.
    private static final class ForeignGraphics implements Graphics {
        public void fillRect(double x, double y, double w, double h,
                org.strangeeons.plugins.graphics.Paint p) {}
        public void drawRect(double x, double y, double w, double h,
                org.strangeeons.plugins.graphics.Stroke s) {}
        public void fillRoundRect(double x, double y, double w, double h,
                double rx, double ry,
                org.strangeeons.plugins.graphics.Paint p) {}
        public void drawRoundRect(double x, double y, double w, double h,
                double rx, double ry,
                org.strangeeons.plugins.graphics.Stroke s) {}
        public void drawLine(double x1, double y1, double x2, double y2,
                org.strangeeons.plugins.graphics.Stroke s) {}
        public void fill(org.strangeeons.plugins.graphics.Shape sh,
                org.strangeeons.plugins.graphics.Paint p) {}
        public void draw(org.strangeeons.plugins.graphics.Shape sh,
                org.strangeeons.plugins.graphics.Stroke s) {}
        public void drawImage(org.strangeeons.plugins.graphics.ImageHandle img,
                double x, double y) {}
        public void drawImage(org.strangeeons.plugins.graphics.ImageHandle img,
                Region region) {}
        public void drawString(String text, double x, double y,
                org.strangeeons.plugins.graphics.Font f,
                org.strangeeons.plugins.graphics.Paint p) {}
        public org.strangeeons.plugins.graphics.FontMetrics measureText(
                String text, org.strangeeons.plugins.graphics.Font f) {
            return null;
        }
        public void withTransform(org.strangeeons.plugins.graphics.Transform t,
                java.util.function.Consumer<Graphics> block) {}
        public void withClip(org.strangeeons.plugins.graphics.Shape clip,
                java.util.function.Consumer<Graphics> block) {}
        public void withAlpha(double alpha,
                java.util.function.Consumer<Graphics> block) {}
        public void withBlendMode(org.strangeeons.plugins.graphics.BlendMode mode,
                java.util.function.Consumer<Graphics> block) {}
    }
}
