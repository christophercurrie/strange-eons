package org.strangeeons.core.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.layout.PageShape;
import ca.cgjennings.layout.TextStyle;
import java.awt.Color;
import java.awt.font.TextAttribute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.strangeeons.core.Host;

class SettingValuesTest {

    private final Host previous = Host.get();

    @AfterEach
    void restoreHost() {
        Host.set(previous);
    }

    @Test
    void readsYesNo() {
        assertThat(SettingValues.yesNo(" Yes ")).isTrue();
        assertThat(SettingValues.yesNo("TRUE")).isTrue();
        assertThat(SettingValues.yesNo("1")).isTrue();
        assertThat(SettingValues.yesNo("no")).isFalse();
        assertThat(SettingValues.yesNo("2")).isFalse();
        assertThat(SettingValues.yesNo(null)).isFalse();
    }

    @Test
    void readsIntegers() {
        assertThat(SettingValues.integer(" 42 ")).isEqualTo(42);
        assertThat(SettingValues.integer("0x10")).isEqualTo(16);
        assertThat(SettingValues.integer("-7.9")).isEqualTo(-7);
        assertThat(SettingValues.integer(".5")).isZero();
        assertThatThrownBy(() -> SettingValues.integer("seven")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> SettingValues.integer("x.5")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> SettingValues.integer(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void readsNumbers() {
        assertThat(SettingValues.number(" 2.5 ")).isEqualTo(2.5);
        assertThatThrownBy(() -> SettingValues.number("two")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void readsCupShapes() {
        PageShape.CupShape cup = SettingValues.cupShape("1, 2, 3.5, 4, 5");
        assertThat(cup).isNotNull();
        assertThatThrownBy(() -> SettingValues.cupShape("1,2,3")).hasMessageContaining("need 5 values");
    }

    @Test
    void readsTextAlignment() {
        assertThat(SettingValues.textAlignment(null))
                .isEqualTo(MarkupRenderer.LAYOUT_TOP | MarkupRenderer.LAYOUT_LEFT);
        assertThat(SettingValues.textAlignment("right, bottom, justified"))
                .isEqualTo(MarkupRenderer.LAYOUT_RIGHT | MarkupRenderer.LAYOUT_BOTTOM | MarkupRenderer.LAYOUT_JUSTIFY);
        assertThat(SettingValues.textAlignment("centre, middle,, ragged"))
                .isEqualTo(MarkupRenderer.LAYOUT_CENTER | MarkupRenderer.LAYOUT_MIDDLE);
        assertThat(SettingValues.textAlignment("CENTER, TOP, unjustified, left"))
                .isEqualTo(MarkupRenderer.LAYOUT_LEFT | MarkupRenderer.LAYOUT_TOP);
        assertThatThrownBy(() -> SettingValues.textAlignment("sideways")).hasMessageContaining("sideways");
    }

    @Test
    void readsTextStyles() {
        TextStyle style = SettingValues.textStyle(
                "family: 'Serif'; size: 12; weight: bold; foreground: #ff0000; tracking: 0.05; posture: italic",
                null);
        assertThat(style.get(TextAttribute.FAMILY)).isEqualTo("Serif");
        assertThat(style.get(TextAttribute.SIZE)).isEqualTo(12);
        assertThat(style.get(TextAttribute.WEIGHT)).isEqualTo(TextAttribute.WEIGHT_BOLD);
        assertThat(style.get(TextAttribute.FOREGROUND)).isEqualTo(new Color(0xffff0000, true));
        assertThat(style.get(TextAttribute.TRACKING)).isEqualTo(0.05f);
        assertThat(style.get(TextAttribute.POSTURE)).isEqualTo(TextAttribute.POSTURE_OBLIQUE);
    }

    @Test
    void addsToABaseStyle() {
        TextStyle base = new TextStyle(TextAttribute.FAMILY, "Dialog");
        assertThat(SettingValues.textStyle("swap colours: on; underline: off; strikethrough:", base)).isSameAs(base);
        assertThat(base.get(TextAttribute.FAMILY)).isEqualTo("Dialog");
        assertThat(base.get(TextAttribute.SWAP_COLORS)).isEqualTo(true);
        assertThat(base.get(TextAttribute.UNDERLINE)).isEqualTo(-1);
    }

    @Test
    void usesTheGivenColourParser() {
        Color marker = new Color(1, 2, 3);
        TextStyle style = SettingValues.textStyle("foreground: rgb(1,0,0)", null, v -> marker);
        assertThat(style.get(TextAttribute.FOREGROUND)).isSameAs(marker);
    }

    @Test
    void handlesQuotedAndBracedValues() {
        TextStyle style = SettingValues.textStyle("family: \"Semi; Colon\"; size: 9", null);
        assertThat(style.get(TextAttribute.FAMILY)).isEqualTo("Semi; Colon");
        assertThat(style.get(TextAttribute.SIZE)).isEqualTo(9);
    }

    @Test
    void scriptLiteralsNeedAScriptHost() {
        assertThatThrownBy(() -> SettingValues.textStyle("size: {12}", null))
                .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("{12}");
        Host.set(new Host() {
            @Override
            public Object evaluateStyleLiteral(String code) {
                return 3f * Float.parseFloat(code.trim());
            }
        });
        assertThat(SettingValues.textStyle("size: { 4 }", null).get(TextAttribute.SIZE)).isEqualTo(12f);
    }

    @Test
    void rejectsMalformedStyles() {
        assertThatThrownBy(() -> SettingValues.textStyle("bogus: 1", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SettingValues.textStyle("weight: size", null)).hasMessageContaining("TextAttribute");
        assertThatThrownBy(() -> SettingValues.textStyle("weight: heavyish", null)).hasMessageContaining("unknown token");
        assertThatThrownBy(() -> SettingValues.textStyle("family: 'unclosed", null)).hasMessageContaining("unclosed");
        assertThatThrownBy(() -> SettingValues.textStyle("size: {1}}", null)).hasMessageContaining("without");
        assertThat(SettingValues.textStyle("no colon here", null)).isNotNull();
    }
}
