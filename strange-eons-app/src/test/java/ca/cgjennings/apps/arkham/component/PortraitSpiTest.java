package ca.cgjennings.apps.arkham.component;

import ca.cgjennings.graphics.spi.AwtImageHandle;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.strangeeons.plugins.graphics.ImageHandle;
import org.strangeeons.plugins.graphics.Region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the default-method bridge on the legacy
 * {@link Portrait} interface satisfies the SPI
 * {@link org.strangeeons.plugins.component.Portrait} contract.
 */
class PortraitSpiTest {

    @Test
    void legacyPortraitImplementsSpi() {
        Portrait legacy = new StubPortrait();
        org.strangeeons.plugins.component.Portrait spi = legacy;
        assertSame(legacy, spi);
    }

    @Test
    void getImageHandleWrapsLegacyImageAndSource() {
        BufferedImage bi = new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB);
        StubPortrait p = new StubPortrait();
        p.image = bi;
        p.source = "resources/test.png";

        ImageHandle handle = p.getImageHandle();
        assertNotNull(handle);
        assertEquals(64, handle.getWidth());
        assertEquals(48, handle.getHeight());
        assertEquals("resources/test.png", handle.getSource());
        assertTrue(handle instanceof AwtImageHandle);
        assertSame(bi, ((AwtImageHandle) handle).image());
    }

    @Test
    void getImageHandleReturnsNullWhenNoImage() {
        StubPortrait p = new StubPortrait();
        p.image = null;
        assertNull(p.getImageHandle());
    }

    @Test
    void setImageHandleRoutesThroughLegacySetImage() {
        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        AwtImageHandle handle = new AwtImageHandle(bi, "set-source");

        StubPortrait p = new StubPortrait();
        p.setImageHandle(handle);

        assertSame(bi, p.lastSetImage);
        assertEquals("set-source", p.lastSetSource);
    }

    @Test
    void setImageHandleWithNullClearsImage() {
        StubPortrait p = new StubPortrait();
        p.image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        p.setImageHandle(null);
        assertNull(p.lastSetImage);
        assertNull(p.lastSetSource);
    }

    @Test
    void setImageHandleRejectsForeignHandle() {
        StubPortrait p = new StubPortrait();
        ImageHandle foreign = new ImageHandle() {
            @Override public int getWidth()    { return 1; }
            @Override public int getHeight()   { return 1; }
            @Override public String getSource() { return null; }
        };
        assertThrows(IllegalArgumentException.class,
                () -> p.setImageHandle(foreign));
    }

    @Test
    void getClipRegionDerivesFromLegacyDimensions() {
        StubPortrait p = new StubPortrait();
        p.clipDimensions = new Dimension(120, 80);
        Region r = p.getClipRegion();
        assertNotNull(r);
        assertEquals(0, r.getX());
        assertEquals(0, r.getY());
        assertEquals(120, r.getWidth());
        assertEquals(80, r.getHeight());
    }

    @Test
    void getClipRegionReturnsNullWhenNoDimensions() {
        StubPortrait p = new StubPortrait();
        p.clipDimensions = null;
        assertNull(p.getClipRegion());
    }

    // Minimal Portrait stub recording legacy setImage calls; field-backed.
    private static final class StubPortrait implements Portrait {
        BufferedImage image;
        String source = "";
        Dimension clipDimensions = new Dimension(1, 1);

        BufferedImage lastSetImage;
        String        lastSetSource;

        @Override public void setSource(String resource) { this.source = resource; }
        @Override public String getSource() { return source; }
        @Override public BufferedImage getImage() { return image; }
        @Override public void setImage(String reportedSource, BufferedImage image) {
            this.lastSetSource = reportedSource;
            this.lastSetImage  = image;
            this.image  = image;
            this.source = reportedSource == null ? "" : reportedSource;
        }
        @Override public double getScale() { return 1d; }
        @Override public void setScale(double scale) {}
        @Override public double getPanX() { return 0d; }
        @Override public void setPanX(double x) {}
        @Override public double getPanY() { return 0d; }
        @Override public void setPanY(double y) {}
        @Override public void setPan(Point2D pan) {}
        @Override public Point2D getPan(Point2D dest) { return new Point2D.Double(); }
        @Override public double getRotation() { return 0d; }
        @Override public void setRotation(double angleInDegrees) {}
        @Override public void installDefault() {}
        @Override public Dimension getClipDimensions() { return clipDimensions; }
        @Override public BufferedImage getClipStencil() { return null; }
        @Override public EnumSet<Feature> getFeatures() { return EnumSet.noneOf(Feature.class); }
    }
}
