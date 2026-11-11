package org.strangeeons.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.cgjennings.layout.MarkupRenderer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.strangeeons.core.Host;
import org.strangeeons.core.graphics.awt.AwtMarkupRenderer;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.ComponentHandler;
import org.strangeeons.plugins.conventional.Face;
import org.strangeeons.plugins.graphics.Graphics;
import org.strangeeons.plugins.graphics.Paint;

class FaceImagesTest {

    @TempDir
    Path tmp;

    private TestResources resources;
    private TestSettings settings;

    /** Two faces: the front paints its template, a box and text; the back only a box. */
    static final class CardHandler implements ComponentHandler {
        int painters;

        @Override
        public int getFaceCount() {
            return 2;
        }

        @Override
        public String getTemplateKey(int faceIndex) {
            return faceIndex == 0 ? "card-front" : null;
        }

        @Override
        public void createPainter(Face face) {
            ++painters;
        }

        @Override
        public void paint(Face face, Graphics g) {
            g.drawImage(face.getTemplateImage(), 0, 0);
            if (face.getFaceIndex() == 0) {
                g.fillRect(face.region("box").getX(), face.region("box").getY(),
                        face.region("box").getWidth(), face.region("box").getHeight(), Paint.color("ff0000"));
                org.strangeeons.plugins.graphics.MarkupRenderer text = face.createMarkupRenderer();
                text.setMarkupText("<b>Hello");
                text.draw(g, face.region("text"));
            }
        }
    }

    @BeforeEach
    void installResources() throws IOException {
        Path res = tmp.resolve("game/resources");
        Files.createDirectories(res.resolve("cards"));
        BufferedImage template = new BufferedImage(100, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = template.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 100, 60);
        g.dispose();
        ImageIO.write(template, "png", res.resolve("cards/front.png").toFile());
        resources = TestResources.install(res);
        settings = new TestSettings();
        settings.set("card-front-template", "cards/front.png");
        settings.set("card-front-ppi", "150");
        settings.set("card-front-box-region", "10,10,20,20");
        settings.set("card-front-text-region", "40,10,55,40");
    }

    @AfterEach
    void removeResources() {
        resources.close();
    }

    @Test
    void paintsFacesAtTheTemplateSize() {
        CardHandler handler = new CardHandler();
        BufferedImage front = FaceImages.render(handler, settings, 0);
        assertEquals(100, front.getWidth());
        assertEquals(60, front.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, front.getType());
        assertEquals(0xffff0000, front.getRGB(15, 15));
        assertEquals(0xffffffff, front.getRGB(5, 55));
        boolean text = false;
        for (int y = 10; y < 50 && !text; ++y) {
            for (int x = 40; x < 95 && !text; ++x) {
                text = front.getRGB(x, y) != 0xffffffff;
            }
        }
        assertTrue(text, "expected text in the text region");
        assertEquals(1, handler.painters);
    }

    @Test
    void scalesToARequestedResolution() {
        BufferedImage front = FaceImages.render(new CardHandler(), settings, 0, 300);
        assertEquals(200, front.getWidth());
        assertEquals(120, front.getHeight());
        assertEquals(0xffff0000, front.getRGB(30, 30));
    }

    @Test
    void missingTemplatesUseTheStandIn() {
        List<BufferedImage> faces = FaceImages.renderAll(new CardHandler(), settings);
        assertEquals(2, faces.size());
        BufferedImage back = faces.get(1);
        assertEquals(Host.get().getMissingImage().getWidth(), back.getWidth());
        assertEquals(Host.get().getMissingImage().getHeight(), back.getHeight());
    }

    @Test
    void facesReadKeysAsTheApplicationDoes() {
        TestFace front = FaceImages.face(new CardHandler(), settings, 0);
        assertEquals("card-front", front.getTemplateKey());
        assertEquals(0, front.getFaceIndex());
        assertEquals(150, front.getTemplateResolution());
        assertEquals(10, front.region("box").getX());
        assertEquals(100, front.getTemplateImage().getWidth());
        assertEquals(Host.get().getMissingImage().getWidth(), front.image("absent").getWidth());
        settings.set("card-front-art", "cards/front.png");
        assertEquals(100, front.image("art").getWidth());
        assertEquals(settings, front.getSettings());

        TestFace back = FaceImages.face(new CardHandler(), settings, 1);
        assertEquals(TestFace.DEFAULT_TEMPLATE_KEY, back.getTemplateKey());
        settings.set("diy-front-sheet-dpi", "300");
        assertEquals(300, back.getTemplateResolution());
        settings.set("card-front-ppi", "72.5");
        assertEquals(72.5, front.getTemplateResolution2D());
        assertEquals(73, front.getTemplateResolution());
        settings.reset("card-front-ppi");
        assertEquals(150, front.getTemplateResolution());

        assertThrows(IllegalArgumentException.class, () -> FaceImages.face(new CardHandler(), settings, 2));
        assertThrows(IllegalArgumentException.class, () -> FaceImages.face(new CardHandler(), settings, -1));
        assertThrows(IllegalArgumentException.class, () -> new TestFace(settings, "k", -1));
        assertThrows(NullPointerException.class, () -> new TestFace(null, "k", 0));
        assertThrows(NullPointerException.class, () -> FaceImages.face(null, settings, 0));
    }

    @Test
    void renderersAreSetUpLikeSheetRenderers() {
        TestFace front = FaceImages.face(new CardHandler(), settings, 0);
        MarkupRenderer r = ((AwtMarkupRenderer) front.createMarkupRenderer()).renderer();
        assertEquals(MarkupRenderer.FIT_BOTH, r.getTextFitting());
        assertTrue(r.getMarkBadBox());
        assertEquals(0.4, r.getScalingLimit(), 1e-9);
        assertEquals(0.3f, r.getTightnessLimit(), 1e-6);

        settings.set("default-text-fitting", "spacing");
        settings.set("highlight-bad-boxes", "no");
        settings.set("min-text-scaling-factor", "0.5");
        r = ((AwtMarkupRenderer) front.createMarkupRenderer()).renderer();
        assertEquals(MarkupRenderer.FIT_TIGHTEN_LINE_SPACING, r.getTextFitting());
        assertNotEquals(true, r.getMarkBadBox());
        assertEquals(0.5, r.getScalingLimit(), 1e-9);

        assertEquals(MarkupRenderer.FIT_NONE, StandardRendering.textFitting(" None "));
        assertEquals(MarkupRenderer.FIT_SCALE_TEXT, StandardRendering.textFitting("scaling"));
        assertEquals(MarkupRenderer.FIT_BOTH, StandardRendering.textFitting("both"));
        assertThrows(IllegalArgumentException.class, () -> StandardRendering.textFitting("squeeze"));
    }

    @Test
    void handlersWithoutPaintingLeaveABlankFace() {
        ComponentHandler blank = new ComponentHandler() {
        };
        SettingsReadWrite s = new TestSettings();
        BufferedImage face = FaceImages.render(blank, s, 0);
        assertEquals(0xff000000, face.getRGB(0, 0));
    }
}
