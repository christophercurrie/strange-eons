package ca.cgjennings.imageio;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class ImageLayerTest {

    @Test
    void everyBlendingModeCreatesAUsableComposite() {
        for (ImageLayer.BlendingMode mode : ImageLayer.BlendingMode.values()) {
            Composite c = mode.createGraphicsComposite(0.5f);
            assertThat(c).as(mode.name()).isNotNull();
            BufferedImage dest = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dest.createGraphics();
            try {
                g.setComposite(c);
                g.drawImage(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), 0, 0, null);
            } finally {
                g.dispose();
            }
        }
    }
}
