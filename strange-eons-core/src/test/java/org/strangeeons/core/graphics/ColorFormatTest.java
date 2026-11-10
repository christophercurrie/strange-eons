package org.strangeeons.core.graphics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ColorFormatTest {

    @Test
    void parsesHex() {
        assertThat(ColorFormat.parse("ff8800")).isEqualTo(0xffff8800);
        assertThat(ColorFormat.parse(" #ff8800 ")).isEqualTo(0xffff8800);
        assertThat(ColorFormat.parse("80ff8800")).isEqualTo(0x80ff8800);
    }

    @Test
    void parsesRgb() {
        assertThat(ColorFormat.parse("rgb(1, 0.5, 0)")).isEqualTo(0xffff8000);
        assertThat(ColorFormat.parse("RGB(0,0,1,0.5)")).isEqualTo(0x800000ff);
        assertThat(ColorFormat.parse("(2, -1, 0)")).isEqualTo(0xffff0000);
    }

    @Test
    void parsesHsb() {
        assertThat(ColorFormat.parse("hsb(120, 1, 1)")).isEqualTo(0xff00ff00);
        // alpha is ignored for hsb colours
        assertThat(ColorFormat.parse("HSB(0, 1, 1, 0.5)")).isEqualTo(0xffff0000);
    }

    @Test
    void rejectsMalformedColours() {
        assertThatThrownBy(() -> ColorFormat.parse("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ColorFormat.parse("rgb 1, 0, 0)")).hasMessageContaining("missing (");
        assertThatThrownBy(() -> ColorFormat.parse("rgb(1, 0, 0")).hasMessageContaining("missing )");
        assertThatThrownBy(() -> ColorFormat.parse("rgb(1, 0)")).hasMessageContaining("missing components");
        assertThatThrownBy(() -> ColorFormat.parse("rgb(1, 0, 0, 1, 1)")).hasMessageContaining("too many");
        assertThatThrownBy(() -> ColorFormat.parse("fff")).hasMessageContaining("6 or 8");
        assertThatThrownBy(() -> ColorFormat.parse("gggggg")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> ColorFormat.parse(null)).isInstanceOf(NullPointerException.class);
    }
}
