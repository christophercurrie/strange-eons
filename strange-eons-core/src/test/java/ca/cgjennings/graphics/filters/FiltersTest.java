package ca.cgjennings.graphics.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class FiltersTest {

    static List<String> filters() {
        return List.of(
                "AlphaInversionFilter", "AlphaStrengthenFilter", "BloomFilter", "BlurFilter",
                "BrightnessContrastFilter", "ChannelFunctionFilter", "ChannelSwapFilter", "CheckeredScreenFilter",
                "ClearFilter", "CloneFilter", "ColorOverlayFilter", "CompoundPixelwiseFilter", "ConvolveFilter",
                "DarkMagicFilter", "GammaCorrectionFilter", "GaussianBlurFilter", "GlowFilter", "GreyscaleFilter",
                "InversionFilter", "MarginFilter", "OilPaintingFilter", "PixelArtUpscalingFilter",
                "ReplaceHueSaturationFilter", "SharpenFilter", "StrokeFilter", "SubstitutionFilter",
                "TintFilter", "TintOverlayFilter", "TrimFilter", "TurnAndFlipFilter");
    }

    static BufferedImage sample(int type) {
        BufferedImage im = new BufferedImage(17, 11, type);
        for (int y = 0; y < im.getHeight(); ++y) {
            for (int x = 0; x < im.getWidth(); ++x) {
                int a = (x == 0 || y == 0) ? 0 : 0xff;
                im.setRGB(x, y, (a << 24) | (x * 15 << 16) | (y * 23 << 8) | ((x + y) * 7));
            }
        }
        return im;
    }

    @ParameterizedTest
    @MethodSource("filters")
    void everyFilterProcessesRgbAndArgbImages(String name) throws Exception {
        BufferedImageOp op = (BufferedImageOp) Class.forName("ca.cgjennings.graphics.filters." + name)
                .getConstructor().newInstance();
        for (int type : new int[]{BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_3BYTE_BGR}) {
            BufferedImage src = sample(type);
            BufferedImage out = op.filter(src, null);
            assertThat(out).as(name).isNotNull();
            assertThat(out.getWidth()).as(name).isPositive();
            assertThat(out.getHeight()).as(name).isPositive();
        }
    }

    @Test
    void greyscaleEqualizesChannels() {
        BufferedImage out = new GreyscaleFilter().filter(sample(BufferedImage.TYPE_INT_ARGB), null);
        int p = out.getRGB(5, 5);
        assertThat((p >> 16) & 0xff).isEqualTo((p >> 8) & 0xff).isEqualTo(p & 0xff);
        assertThat(out.getRGB(0, 0) >>> 24).isZero();
    }

    @Test
    void inversionInvertsColorAndKeepsAlpha() {
        BufferedImage src = sample(BufferedImage.TYPE_INT_ARGB);
        BufferedImage out = new InversionFilter().filter(src, null);
        int s = src.getRGB(5, 5);
        int o = out.getRGB(5, 5);
        assertThat(o & 0xffffff).isEqualTo(~s & 0xffffff);
        assertThat(o >>> 24).isEqualTo(s >>> 24);
    }

    @Test
    void identityTintLeavesPixelsAlone() {
        TintFilter tint = new TintFilter();
        tint.setFactors(0f, 1f, 1f);
        assertThat(tint.isIdentity()).isTrue();
        BufferedImage src = sample(BufferedImage.TYPE_INT_ARGB);
        BufferedImage out = tint.filter(src, null);
        assertThat(out.getRGB(5, 5)).isEqualTo(src.getRGB(5, 5));
        tint.setFactors(0.5f, 1f, 1f);
        assertThat(tint.isIdentity()).isFalse();
        assertThat(tint.getHFactor()).isEqualTo(0.5f);
    }

    @Test
    void tintCacheReusesTintedImageUntilChanged() {
        TintFilter tint = new TintFilter(0.25f, 1f, 1f);
        BufferedImage src = sample(BufferedImage.TYPE_INT_ARGB);
        TintCache cache = new TintCache(tint, src);
        BufferedImage first = cache.getTintedImage();
        assertThat(cache.getTintedImage()).isSameAs(first);
        assertThat(cache.getFilter()).isSameAs(tint);
        assertThat(cache.getImage()).isSameAs(src);
        int before = first.getRGB(5, 5);
        cache.setFactors(0.5f, 1f, 1f);
        assertThat(cache.getTintedImage()).isSameAs(first);
        assertThat(first.getRGB(5, 5)).isNotEqualTo(before);
        assertThat(cache.getHFactor()).isEqualTo(0.5f);
        cache.setFactors(0f, 1f, 1f);
        assertThat(cache.getTintedImage()).isSameAs(src);
    }

    @Test
    void turnsSwapDimensionsAndMovePixels() {
        BufferedImage src = sample(BufferedImage.TYPE_INT_ARGB);
        BufferedImage left = new TurnAndFlipFilter(TurnAndFlipFilter.TURN_90_LEFT).filter(src, null);
        assertThat(left.getWidth()).isEqualTo(src.getHeight());
        assertThat(left.getHeight()).isEqualTo(src.getWidth());
        BufferedImage half = new TurnAndFlipFilter(TurnAndFlipFilter.TURN_180).filter(src, null);
        assertThat(half.getRGB(0, 0)).isEqualTo(src.getRGB(src.getWidth() - 1, src.getHeight() - 1));
    }

    @Test
    void trimRemovesTransparentEdges() {
        BufferedImage src = new BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(3, 2, 0xff000000);
        src.setRGB(6, 5, 0xff000000);
        BufferedImage out = new TrimFilter().filter(src, null);
        assertThat(out.getWidth()).isEqualTo(4);
        assertThat(out.getHeight()).isEqualTo(4);
    }

    @Test
    void stencilTakesAlphaFromStencil() {
        BufferedImage src = sample(BufferedImage.TYPE_INT_ARGB);
        BufferedImage stencil = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        stencil.setRGB(5, 5, 0xff000000);
        BufferedImage out = new StencilFilter(stencil).filter(src, null);
        assertThat(out.getRGB(5, 5) >>> 24).isEqualTo(0xff);
        assertThat(out.getRGB(6, 6) >>> 24).isZero();
    }

    @Test
    void cloneCopiesPixels() {
        BufferedImage src = sample(BufferedImage.TYPE_INT_ARGB);
        BufferedImage out = new CloneFilter().filter(src, null);
        assertThat(out).isNotSameAs(src);
        assertThat(out.getRGB(7, 7)).isEqualTo(src.getRGB(7, 7));
    }
}
