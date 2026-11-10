package org.strangeeons.core.graphics;

import static org.assertj.core.api.Assertions.assertThat;

import ca.cgjennings.graphics.shapes.VectorImage;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class MissingImagesTest {

    @Test
    void drawsACrossedOutSquareInTheGivenColors() {
        VectorImage vi = MissingImages.create(Color.GREEN, Color.BLUE);
        assertThat(vi.getWidth()).isEqualTo(17);
        assertThat(vi.getHeight()).isEqualTo(17);
        BufferedImage bi = vi.createRasterImage(170, 170, true);
        assertThat(bi.getRGB(40, 85) & 0xffffff).isEqualTo(0x00ff00);
        assertThat(bi.getRGB(85, 85) & 0xffffff).isEqualTo(0x0000ff);
    }

    @Test
    void defaultUsesPinkAndRed() {
        BufferedImage bi = MissingImages.createDefault().createRasterImage(170, 170, true);
        assertThat(bi.getRGB(40, 85) & 0xffffff).isEqualTo(0xf8bbd0);
    }
}
