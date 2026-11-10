package org.strangeeons.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.cgjennings.graphics.shapes.AbstractVectorImage;
import ca.cgjennings.graphics.shapes.VectorImage;
import ca.cgjennings.layout.FontFamilyStyleFactory;
import ca.cgjennings.layout.GenderAwareMarkupRenderer;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import resources.StrangeImage;

class HostTest {

    @TempDir
    Path tmp;

    private Host previous;

    @BeforeEach
    void save() {
        previous = Host.get();
    }

    @AfterEach
    void restore() {
        Host.set(previous);
    }

    private static VectorImage square(double size) {
        return new AbstractVectorImage() {
            {
                tx = ty = 0;
                iw = ih = size;
            }

            @Override
            protected void render(Graphics2D g) {
                g.fillRect(0, 0, (int) size, (int) size);
            }
        };
    }

    @Test
    void defaultsWorkWithoutTheApplication() throws IOException {
        Host host = new Host() {
        };
        assertThat(host.prepareImage(null)).isNull();
        BufferedImage bgr = new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);
        assertThat(host.prepareImage(bgr).getType()).isEqualTo(BufferedImage.TYPE_INT_RGB);
        BufferedImage argb = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        assertThat(host.prepareImage(argb)).isSameAs(argb);
        assertThat(host.loadVectorImage(new URL("file:/x.svg"))).isNull();
        assertThat(host.getMissingVectorImage().getWidth()).isEqualTo(17);
        assertThat(host.getMissingImage().getWidth()).isEqualTo(64);
        assertThat(host.getMissingImage()).isSameAs(host.getMissingImage());
        assertThat(host.resolveFontFamily(null)).isEmpty();
        assertThat(host.resolveFontFamily("  Serif ")).isEqualTo("Serif");
        assertThat(host.resolveFontFamily("cloud: Roboto")).isEqualTo("Roboto");
        assertThat(host.getGameLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void missingBitmapFollowsAReplacedVector() {
        VectorImage custom = square(8);
        Host host = new Host() {
            @Override
            public VectorImage getMissingVectorImage() {
                return custom;
            }
        };
        assertThat(host.getMissingImage().getWidth()).isEqualTo(64);
        assertThat(host.getMissingImage()).isNotSameAs(new Host() {
        }.getMissingImage());
    }

    @Test
    void installedHostIsUsed() {
        Host host = new Host() {
        };
        Host.set(host);
        assertThat(Host.get()).isSameAs(host);
        assertThatThrownBy(() -> Host.set(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void strangeImageLoadsVectorsThroughTheHost() throws IOException {
        Path svg = tmp.resolve("shape.svg");
        Files.writeString(svg, "<svg/>");
        VectorImage loaded = square(20);
        Host.set(new Host() {
            @Override
            public VectorImage loadVectorImage(URL url) {
                return loaded;
            }
        });
        StrangeImage si = StrangeImage.get(svg.toString());
        assertThat(si.isVectorFormat()).isTrue();
        assertThat(si.asVectorImage()).isSameAs(loaded);
    }

    @Test
    void unsupportedVectorsAreMissing() throws IOException {
        Path svg = tmp.resolve("unsupported.svg");
        Files.writeString(svg, "<svg/>");
        Host.set(new Host() {
        });
        assertThat(StrangeImage.get(svg.toString())).isSameAs(StrangeImage.getMissingImage());
    }

    @Test
    void failedVectorLoadsAreMissing() throws IOException {
        Path svg = tmp.resolve("broken.svg");
        Files.writeString(svg, "<svg/>");
        Host.set(new Host() {
            @Override
            public VectorImage loadVectorImage(URL url) throws IOException {
                throw new IOException("unreadable");
            }
        });
        assertThat(StrangeImage.get(svg.toString())).isSameAs(StrangeImage.getMissingImage());
    }

    @Test
    void bitmapsArePreparedByTheHost() throws IOException {
        Path png = tmp.resolve("a.png");
        javax.imageio.ImageIO.write(new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", png.toFile());
        BufferedImage marker = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        Host.set(new Host() {
            @Override
            public BufferedImage prepareImage(Image image) {
                return marker;
            }
        });
        assertThat(StrangeImage.get(png.toString()).asBufferedImage()).isSameAs(marker);
    }

    @Test
    void missingImageFollowsTheHost() {
        VectorImage custom = square(9);
        Host.set(new Host() {
        });
        StrangeImage standard = StrangeImage.getMissingImage();
        Host.set(new Host() {
            @Override
            public VectorImage getMissingVectorImage() {
                return custom;
            }
        });
        assertThat(StrangeImage.getMissingImage()).isNotSameAs(standard);
        assertThat(StrangeImage.getMissingImage().asVectorImage()).isSameAs(custom);
        assertThat(StrangeImage.getAsBufferedImage(tmp.resolve("none.png").toString()).getWidth()).isEqualTo(64);
    }

    @Test
    void layoutUsesHostFontsAndLocale() {
        Host.set(new Host() {
            @Override
            public String resolveFontFamily(String family) {
                return "Resolved";
            }

            @Override
            public Locale getGameLocale() {
                return Locale.forLanguageTag("tr");
            }
        });
        assertThat(FontFamilyStyleFactory.getShared().createStyle(null, new String[]{"anything"}).get(TextAttribute.FAMILY))
                .isEqualTo("Resolved");
        assertThat(GenderAwareMarkupRenderer.capitalize("istanbul")).isEqualTo("İstanbul");
    }
}
