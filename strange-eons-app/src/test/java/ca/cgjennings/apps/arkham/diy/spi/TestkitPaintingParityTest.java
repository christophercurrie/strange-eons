package ca.cgjennings.apps.arkham.diy.spi;

import static org.assertj.core.api.Assertions.assertThat;

import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.ComponentHandler;
import org.strangeeons.plugins.conventional.Face;
import org.strangeeons.plugins.graphics.Graphics;
import org.strangeeons.plugins.graphics.MarkupRenderer;
import org.strangeeons.plugins.graphics.Paint;
import org.strangeeons.testkit.FaceImages;
import org.strangeeons.testkit.TestSettings;
import resources.RawSettings;

/**
 * Paints the same handler through a real component sheet and through the
 * testkit, which must produce the same pixels.
 */
class TestkitPaintingParityTest {

    @TempDir
    static Path tmp;

    /** Paints its template, a filled box, and fitted markup text. */
    public static final class ParityHandler implements ComponentHandler {
        @Override
        public String getTemplateKey(int faceIndex) {
            return "parity-front";
        }

        @Override
        public void paint(Face face, Graphics g) {
            g.drawImage(face.getTemplateImage(), 0, 0);
            var box = face.region("box");
            g.fillRect(box.getX(), box.getY(), box.getWidth(), box.getHeight(), Paint.color("cc2200"));
            MarkupRenderer text = face.createMarkupRenderer();
            text.setMarkupText(face.getSettings().get("parity-text"));
            text.draw(g, face.region("text"));
        }
    }

    private static Map<String, String> values;

    @BeforeAll
    static void createTemplate() throws Exception {
        RawSettings.loadGlobalSettings("default.settings");
        BufferedImage template = new BufferedImage(240, 160, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = template.createGraphics();
        g.setPaint(new Color(0xf4e8c8));
        g.fillRect(0, 0, 240, 160);
        g.setPaint(new Color(0x553311));
        g.drawRect(4, 4, 231, 151);
        g.dispose();
        Path file = tmp.resolve("parity-template.png");
        ImageIO.write(template, "png", file.toFile());
        values = Map.of(
                "parity-front-template", file.toUri().toString(),
                "parity-front-ppi", "150",
                "parity-front-box-region", "12,12,40,30",
                "parity-front-text-region", "60,12,170,130",
                "parity-text", "<b>Parity</b> check: the testkit and the application lay out "
                + "<i>this markup</i> the same way, including text that must shrink to fit.");
    }

    private static void apply(SettingsReadWrite settings) {
        values.forEach(settings::set);
    }

    @Test
    void testkitPaintsTheSamePixelsAsTheApplication() throws Exception {
        DIY diy = new DIY(ParityHandler.class.getName(), null, false);
        apply(diy.getSettings());
        Sheet<?>[] sheets = diy.createDefaultSheets();
        BufferedImage app = sheets[0].paint(RenderTarget.EXPORT, 150d);

        TestSettings settings = new TestSettings();
        apply(settings);
        BufferedImage testkit = FaceImages.render(new ParityHandler(), settings, 0);

        assertThat(app.getRGB(30, 25)).as("box").isEqualTo(0xffcc2200);
        assertThat(app.getRGB(100, 150)).as("template").isEqualTo(0xfff4e8c8);
        int ink = 0;
        for (int y = 12; y < 142; ++y) {
            for (int x = 60; x < 230; ++x) {
                if (app.getRGB(x, y) != 0xfff4e8c8) {
                    ++ink;
                }
            }
        }
        assertThat(ink).as("text pixels").isGreaterThan(200);

        assertThat(testkit.getWidth()).isEqualTo(app.getWidth());
        assertThat(testkit.getHeight()).isEqualTo(app.getHeight());
        int differing = 0;
        for (int y = 0; y < app.getHeight(); ++y) {
            for (int x = 0; x < app.getWidth(); ++x) {
                if (app.getRGB(x, y) != testkit.getRGB(x, y)) {
                    ++differing;
                }
            }
        }
        assertThat(differing).as("pixels that differ").isZero();
    }

    @Test
    void upsampledRendersMatchToo() throws Exception {
        DIY diy = new DIY(ParityHandler.class.getName(), null, false);
        apply(diy.getSettings());
        BufferedImage app = diy.createDefaultSheets()[0].paint(RenderTarget.EXPORT, 300d);
        TestSettings settings = new TestSettings();
        apply(settings);
        BufferedImage testkit = FaceImages.render(new ParityHandler(), settings, 0, 300d);
        assertThat(app.getWidth()).isEqualTo(480);
        assertThat(testkit.getWidth()).isEqualTo(app.getWidth());
        assertThat(testkit.getHeight()).isEqualTo(app.getHeight());
        int differing = 0;
        for (int y = 0; y < app.getHeight(); ++y) {
            for (int x = 0; x < app.getWidth(); ++x) {
                if (app.getRGB(x, y) != testkit.getRGB(x, y)) {
                    ++differing;
                }
            }
        }
        assertThat(differing).as("pixels that differ at 300 ppi").isZero();
    }
}
