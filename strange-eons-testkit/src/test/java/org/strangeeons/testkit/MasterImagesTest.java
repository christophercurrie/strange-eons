package org.strangeeons.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MasterImagesTest {

    @TempDir
    Path tmp;

    @AfterEach
    void stopUpdating() {
        System.clearProperty(MasterImages.UPDATE_PROPERTY);
    }

    private static BufferedImage image(int rgb) {
        BufferedImage im = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        im.setRGB(2, 2, rgb);
        return im;
    }

    @Test
    void recordsMastersWhenUpdatingThenCompares() throws IOException {
        MasterImages masters = new MasterImages(tmp.resolve("masters"), tmp.resolve("out"));
        assertFalse(MasterImages.isUpdating());
        System.setProperty(MasterImages.UPDATE_PROPERTY, "true");
        assertTrue(MasterImages.isUpdating());
        ImageComparator.Result recorded = masters.check("cards/title", image(0xff00ff00));
        assertEquals(ImageComparator.Kind.MATCH, recorded.kind());
        assertEquals("master updated", recorded.detail());
        assertTrue(Files.exists(tmp.resolve("masters/cards/title.png")));
        System.clearProperty(MasterImages.UPDATE_PROPERTY);

        masters.verify("cards/title", image(0xff00ff00));
        assertTrue(Files.exists(tmp.resolve("out/actual/cards/title.png")));

        AssertionError e = assertThrows(AssertionError.class, () -> masters.verify("cards/title", image(0xffff0000)));
        assertTrue(e.getMessage().contains("cards/title.png: DIFFERENT"), e.getMessage());
        assertTrue(e.getMessage().contains(MasterImages.UPDATE_PROPERTY), e.getMessage());
        assertTrue(Files.exists(tmp.resolve("out/diffs/cards/title.png")));
    }

    @Test
    void missingMastersFail() throws IOException {
        MasterImages masters = new MasterImages(tmp.resolve("masters"), tmp.resolve("out"));
        assertEquals(ImageComparator.Kind.NO_MASTER, masters.check("none", image(0)).kind());
        assertThrows(AssertionError.class, () -> masters.verify("none", image(0)));
        assertThrows(NullPointerException.class, () -> new MasterImages(null, tmp));
        assertThrows(NullPointerException.class, () -> new MasterImages(tmp, null));
    }
}
