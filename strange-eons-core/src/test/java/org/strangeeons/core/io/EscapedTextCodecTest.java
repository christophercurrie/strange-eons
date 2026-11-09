package org.strangeeons.core.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.strangeeons.core.io.EscapedTextCodec.ASSIGNMENT;
import static org.strangeeons.core.io.EscapedTextCodec.BACKSLASH;
import static org.strangeeons.core.io.EscapedTextCodec.INITIAL_COMMENT;
import static org.strangeeons.core.io.EscapedTextCodec.SPACE;
import static org.strangeeons.core.io.EscapedTextCodec.UNICODE;
import static org.strangeeons.core.io.EscapedTextCodec.USE_S_ESCAPE;
import static org.strangeeons.core.io.EscapedTextCodec.WHITESPACE;

import org.junit.jupiter.api.Test;

class EscapedTextCodecTest {

    @Test
    void detectsWrappedLines() {
        assertThat(EscapedTextCodec.isWrappedLine("")).isFalse();
        assertThat(EscapedTextCodec.isWrappedLine("abc")).isFalse();
        assertThat(EscapedTextCodec.isWrappedLine("abc\\")).isTrue();
        assertThat(EscapedTextCodec.isWrappedLine("abc\\\\")).isFalse();
        assertThat(EscapedTextCodec.isWrappedLine("abc\\\\\\")).isTrue();
        assertThatThrownBy(() -> EscapedTextCodec.isWrappedLine(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void unchangedInputIsReturnedAsIs() {
        String s = "plain text";
        assertThat(EscapedTextCodec.escape(s, UNICODE | WHITESPACE | BACKSLASH)).isSameAs(s);
        assertThat(EscapedTextCodec.escape(new StringBuilder("x y"), 0)).isEqualTo("x y");
        assertThat(EscapedTextCodec.unescape(s)).isSameAs(s);
        assertThatThrownBy(() -> EscapedTextCodec.escape(null, UNICODE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> EscapedTextCodec.unescape(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void escapesEachFlag() {
        assertThat(EscapedTextCodec.escapeUnicode("é\u0001")).isEqualTo("\\u00e9\\u0001");
        assertThat(EscapedTextCodec.escape("a b", SPACE)).isEqualTo("a\\ b");
        assertThat(EscapedTextCodec.escape("a b", SPACE | USE_S_ESCAPE)).isEqualTo("a\\sb");
        assertThat(EscapedTextCodec.escape("a=b:c", ASSIGNMENT)).isEqualTo("a\\=b\\:c");
        assertThat(EscapedTextCodec.escape("  #a #b", INITIAL_COMMENT)).isEqualTo("  \\#a #b");
        assertThat(EscapedTextCodec.escape("!a", INITIAL_COMMENT)).isEqualTo("\\!a");
        assertThat(EscapedTextCodec.escape("\r\n\t\f", WHITESPACE)).isEqualTo("\\r\\n\\t\\f");
        assertThat(EscapedTextCodec.escape("a\\b", BACKSLASH)).isEqualTo("a\\\\b");
    }

    @Test
    void unflaggedCharactersPassThrough() {
        String s = "\\x = #y:\r\n\t\f é";
        assertThat(EscapedTextCodec.escape(s, UNICODE)).isEqualTo("\\x = #y:\r\n\t\f \\u00e9");
        assertThat(EscapedTextCodec.escape("é \t", SPACE)).isEqualTo("é\\ \t");
    }

    @Test
    void trailingBackslashIsNotEscaped() {
        assertThat(EscapedTextCodec.escape("a\\b\\", BACKSLASH)).isEqualTo("a\\\\b\\");
    }

    @Test
    void unescapesWhatItEscapes() {
        String s = "tab\tnew\nret\rff\f space é ✓";
        int flags = UNICODE | SPACE | USE_S_ESCAPE | WHITESPACE;
        assertThat(EscapedTextCodec.unescape(EscapedTextCodec.escape(s, flags))).isEqualTo(s);
        assertThat(EscapedTextCodec.unescape("a\\sb")).isEqualTo("a b");
    }

    @Test
    void unknownEscapesPassThroughUnescape() {
        assertThat(EscapedTextCodec.unescape("\\\\ \\= \\: \\# \\  \\")).isEqualTo("\\\\ \\= \\: \\# \\  \\");
    }

    @Test
    void unescapeUnicodeLeavesOtherEscapes() {
        assertThat(EscapedTextCodec.unescapeUnicode("\\u00e9\\n\\u00C9")).isEqualTo("é\\nÉ");
    }

    @Test
    void rejectsInvalidUnicodeEscapes() {
        assertThatThrownBy(() -> EscapedTextCodec.unescape("\\u00g0")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\\u00g0");
    }
}
