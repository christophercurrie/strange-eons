package ca.cgjennings.graphics.composites;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlendModeTest {

    private static BufferedImage solid(int argb) {
        BufferedImage im = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; ++y) {
            for (int x = 0; x < 4; ++x) {
                im.setRGB(x, y, argb);
            }
        }
        return im;
    }

    private static int blend(Composite mode, int dest, int src) {
        BufferedImage d = solid(dest);
        Graphics2D g = d.createGraphics();
        try {
            g.setComposite(mode);
            g.drawImage(solid(src), 0, 0, null);
        } finally {
            g.dispose();
        }
        return d.getRGB(1, 1);
    }

    @Test
    void everyBlendModeComposites() throws IllegalAccessException {
        List<String> names = new ArrayList<>();
        for (Field f : BlendMode.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Composite.class.isAssignableFrom(f.getType())) {
                Composite mode = (Composite) f.get(null);
                blend(mode, 0xff336699, 0x80cc8844);
                names.add(f.getName());
            }
        }
        assertThat(names).contains("Normal", "Lighten", "Darken", "Add", "Multiply", "Screen");
    }

    private static BufferedImage gradient(boolean vertical) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                int v = (vertical ? y : x) * 17;
                int a = (vertical ? x : y) * 17;
                im.setRGB(x, y, (a << 24) | (v << 16) | ((255 - v) << 8) | (v ^ 0x5a));
            }
        }
        return im;
    }

    @Test
    void everyBlendModeHandlesTheFullRange() throws IllegalAccessException {
        for (Field f : BlendMode.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Composite.class.isAssignableFrom(f.getType())) {
                for (int destType : new int[]{BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_RGB}) {
                    BufferedImage d = new BufferedImage(16, 16, destType);
                    Graphics2D g = d.createGraphics();
                    try {
                        g.drawImage(gradient(false), 0, 0, null);
                        g.setComposite((Composite) f.get(null));
                        g.drawImage(gradient(true), 0, 0, null);
                    } finally {
                        g.dispose();
                    }
                    assertThat(d.getWidth()).as(f.getName()).isEqualTo(16);
                }
            }
        }
    }

    @Test
    void lightenAndDarkenPickChannels() {
        assertThat(blend(BlendMode.Lighten, 0xff204060, 0xff602040) & 0xffffff).isEqualTo(0x604060);
        assertThat(blend(BlendMode.Darken, 0xff204060, 0xff602040) & 0xffffff).isEqualTo(0x202040);
    }

    @Test
    void maskCompositeComposites() {
        blend(new MaskComposite(), 0xff336699, 0x80cc8844);
    }
}
