package org.strangeeons.core.graphics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Image;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class ImageFormatsTest {

    private static BufferedImage image(int type) {
        BufferedImage im = new BufferedImage(3, 2, type);
        im.setRGB(1, 1, 0xff336699);
        return im;
    }

    @Test
    void compatibleFormatsFollowTransparency() {
        assertThat(ImageFormats.createCompatibleIntRGBFormat(image(BufferedImage.TYPE_3BYTE_BGR)).getType())
                .isEqualTo(BufferedImage.TYPE_INT_RGB);
        BufferedImage argb = ImageFormats.createCompatibleIntRGBFormat(image(BufferedImage.TYPE_4BYTE_ABGR), 5, 7);
        assertThat(argb.getType()).isEqualTo(BufferedImage.TYPE_INT_ARGB);
        assertThat(argb.getWidth()).isEqualTo(5);
        assertThat(argb.getHeight()).isEqualTo(7);
        assertThat(ImageFormats.createCompatibleIntARGBFormat(image(BufferedImage.TYPE_INT_RGB)).getType())
                .isEqualTo(BufferedImage.TYPE_INT_ARGB);
    }

    @Test
    void ensureIntRgbConvertsOnlyWhenNeeded() {
        BufferedImage rgb = image(BufferedImage.TYPE_INT_RGB);
        assertThat(ImageFormats.ensureIntRGBFormat(rgb)).isSameAs(rgb);
        BufferedImage pre = image(BufferedImage.TYPE_INT_ARGB_PRE);
        assertThat(ImageFormats.ensureIntRGBFormat(pre)).isSameAs(pre);
        BufferedImage converted = ImageFormats.ensureIntRGBFormat(image(BufferedImage.TYPE_3BYTE_BGR));
        assertThat(converted.getType()).isEqualTo(BufferedImage.TYPE_INT_RGB);
        assertThat(converted.getRGB(1, 1)).isEqualTo(0xff336699);
        assertThatThrownBy(() -> ImageFormats.ensureIntRGBFormat(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void ensureIntRgbDrawsNonBufferedImages() {
        Image scaled = image(BufferedImage.TYPE_INT_ARGB).getScaledInstance(6, 4, Image.SCALE_FAST);
        BufferedImage out = ImageFormats.ensureIntRGBFormat(scaled);
        assertThat(out.getType()).isEqualTo(BufferedImage.TYPE_INT_ARGB);
        assertThat(out.getWidth()).isEqualTo(6);
        assertThat(ImageFormats.waitForImage(scaled)).isSameAs(scaled);
    }

    @Test
    void ensuresTypesAndAlpha() {
        BufferedImage argb = image(BufferedImage.TYPE_INT_ARGB);
        assertThat(ImageFormats.ensureImageHasType(argb, BufferedImage.TYPE_INT_ARGB)).isSameAs(argb);
        assertThat(ImageFormats.ensureImageHasType(argb, BufferedImage.TYPE_INT_RGB).getType())
                .isEqualTo(BufferedImage.TYPE_INT_RGB);
        assertThat(ImageFormats.ensureImageHasAlphaChannel(argb)).isSameAs(argb);
        BufferedImage pre = image(BufferedImage.TYPE_INT_ARGB_PRE);
        assertThat(ImageFormats.ensureImageHasAlphaChannel(pre)).isSameAs(pre);
        assertThat(ImageFormats.ensureImageHasAlphaChannel(image(BufferedImage.TYPE_INT_RGB)).getType())
                .isEqualTo(BufferedImage.TYPE_INT_ARGB);
    }

    @Test
    void premultipliesOnlyTransparentImages() {
        assertThat(ImageFormats.ensurePremultipliedFormat(image(BufferedImage.TYPE_INT_ARGB)).getType())
                .isEqualTo(BufferedImage.TYPE_INT_ARGB_PRE);
        assertThat(ImageFormats.ensurePremultipliedFormat(image(BufferedImage.TYPE_3BYTE_BGR)).getType())
                .isEqualTo(BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void bufferedImagesAreAlreadyLoaded() {
        BufferedImage im = image(BufferedImage.TYPE_INT_RGB);
        assertThat(ImageFormats.waitForImage(im)).isSameAs(im);
    }
}
