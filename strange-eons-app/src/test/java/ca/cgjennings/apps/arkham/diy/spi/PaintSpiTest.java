package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYSheet;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import org.strangeeons.core.graphics.awt.AwtImageHandle;
import ca.cgjennings.layout.PageShape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.strangeeons.plugins.SettingsReader;
import org.strangeeons.plugins.graphics.ImageHandle;
import org.strangeeons.plugins.graphics.MarkupRenderer;
import org.strangeeons.plugins.graphics.TextStyle;
import resources.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Covers the text and image lookups a handler uses to paint through the
 * SPI, backed by real settings and renderers.
 */
class PaintSpiTest {

    private Settings settings;
    private SettingsReader reader;

    @BeforeEach
    void setUp() {
        settings = new Settings();
        reader = settings;
    }

    @Test
    void textStyleIsParsedWithOrWithoutSuffix() {
        settings.set("name-style", "FAMILY: \"Serif\"");

        assertThat(reader.readTextStyle("name", null).getFamily()).isEqualTo("Serif");
        assertThat(reader.readTextStyle("name-style", null).getFamily()).isEqualTo("Serif");
    }

    @Test
    void textStyleLayersOverBase() {
        settings.set("base-style", "FAMILY: \"Serif\"");
        settings.set("top-style", "FAMILY: \"SansSerif\"");
        TextStyle base = reader.readTextStyle("base", null);

        TextStyle layered = reader.readTextStyle("top", base);

        assertThat(layered).isSameAs(base);
        assertThat(layered.getFamily()).isEqualTo("SansSerif");
    }

    @Test
    void missingTextStyleReturnsBase() {
        TextStyle base = new ca.cgjennings.layout.TextStyle();

        assertThat(reader.readTextStyle("none", base)).isSameAs(base);
        assertThat(reader.readTextStyle("none", null)).isNull();
    }

    @Test
    void familyCanBeReplaced() {
        settings.set("name-style", "FAMILY: \"Serif\"");
        TextStyle style = reader.readTextStyle("name", null);

        style.setFamily("Chosen");

        assertThat(style.getFamily()).isEqualTo("Chosen");
    }

    @Test
    void unspecifiedFamilyIsNull() {
        assertThat(new ca.cgjennings.layout.TextStyle().getFamily()).isNull();
    }

    @Test
    void pageShapeIsParsedOrNull() {
        settings.set("lore-shape", "1,2,30,4,5");

        assertThat(reader.readPageShape("lore")).isInstanceOf(PageShape.CupShape.class);
        assertThat(reader.readPageShape("none")).isNull();
    }

    @Test
    void alignmentIsParsedOrDefaulted() {
        settings.set("name-alignment", "center, middle");

        assertThat(reader.readAlignment("name"))
                .isEqualTo(MarkupRenderer.LAYOUT_CENTER | MarkupRenderer.LAYOUT_MIDDLE);
        assertThat(reader.readAlignment("none"))
                .isEqualTo(MarkupRenderer.LAYOUT_LEFT | MarkupRenderer.LAYOUT_TOP);
    }

    @Test
    void invalidValuesAreIllegalArguments() {
        settings.set("bad-style", "NOT_A_STYLE: nope");
        settings.set("bad-shape", "not a shape");
        settings.set("bad-alignment", "sideways");

        assertThatIllegalArgumentException().isThrownBy(() -> reader.readTextStyle("bad", null));
        assertThatIllegalArgumentException().isThrownBy(() -> reader.readPageShape("bad"));
        assertThatIllegalArgumentException().isThrownBy(() -> reader.readAlignment("bad"));
    }

    @Test
    void missingImageIsNull() {
        assertThat(reader.readImage("none")).isNull();
    }

    @Test
    void imageIsLoadedFromValue() {
        settings.set("art", "no-such-image-falls-back-to-placeholder.png");

        ImageHandle image = reader.readImage("art");

        assertThat(image).isInstanceOf(AwtImageHandle.class);
        assertThat(image.getWidth()).isPositive();
    }

    @Test
    void faceCreatesRendererAtTemplateResolution() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getTemplateResolution()).thenReturn(150d);

        try (MockedStatic<Sheet> sheets = mockStatic(Sheet.class)) {
            MarkupRenderer renderer = new ConventionalDIYFace(sheet).createMarkupRenderer();

            sheets.verify(() -> Sheet.doStandardRendererInitialization(
                    (ca.cgjennings.layout.MarkupRenderer) renderer));
            assertThat(renderer).isInstanceOf(ca.cgjennings.layout.MarkupRenderer.class);
        }
    }

    @Test
    void replacementTagGoesThroughSpi() {
        MarkupRenderer renderer = new ca.cgjennings.layout.MarkupRenderer(150d);

        renderer.setReplacementForTag("fdk", "<image res://x.png>");

        assertThat(((ca.cgjennings.layout.MarkupRenderer) renderer).getReplacementForTag("fdk"))
                .isEqualTo("<image res://x.png>");
    }

    @Test
    void nullShapeRestoresRectangle() {
        MarkupRenderer renderer = new ca.cgjennings.layout.MarkupRenderer(150d);
        renderer.setPageShape(reader.readPageShape("none"));

        assertThat(renderer.getPageShape()).isSameAs(PageShape.RECTANGLE_SHAPE);
    }
}
