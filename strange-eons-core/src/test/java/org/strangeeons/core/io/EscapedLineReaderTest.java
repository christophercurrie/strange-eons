package org.strangeeons.core.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EscapedLineReaderTest {

    @TempDir
    Path tmp;

    private static EscapedLineReader reader(String text) {
        return new EscapedLineReader(new StringReader(text));
    }

    private static List<String> lines(String text) throws IOException {
        List<String> out = new ArrayList<>();
        try (EscapedLineReader r = reader(text)) {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                out.add(line);
            }
        }
        return out;
    }

    @Test
    void trimsLinesAndSkipsComments() throws IOException {
        assertThat(lines("  one  \n# comment\n! also\n\n two")).containsExactly("one", "", "two");
    }

    @Test
    void joinsContinuedLines() throws IOException {
        assertThat(lines("first \\\n   second \\\n third\nnext")).containsExactly("first second third", "next");
    }

    @Test
    void continuationSkipsCommentLines() throws IOException {
        assertThat(lines("a\\\n# skipped \\\nb")).containsExactly("ab");
    }

    @Test
    void continuationAtEndOfStreamEndsLine() throws IOException {
        assertThat(lines("a\\")).containsExactly("a");
    }

    @Test
    void escapedBackslashDoesNotContinue() throws IOException {
        assertThat(lines("a\\\\\nb")).containsExactly("a\\\\", "b");
    }

    @Test
    void unescapesSequences() {
        assertThat(EscapedLineReader.unescapeLine("a\\nb\\tc\\rd\\fe\\sf\\ g")).isEqualTo("a\nb\tc\rd\fe f g");
        assertThat(EscapedLineReader.unescapeLine("\\u00e9\\u00C9")).isEqualTo("éÉ");
    }

    @Test
    void keepsEscapedBackslashesForLaterProcessing() {
        assertThat(EscapedLineReader.unescapeLine("\\\\")).isEqualTo("\\\\");
    }

    @Test
    void keepsUnknownEscapes() {
        assertThat(EscapedLineReader.unescapeLine("a\\qb")).isEqualTo("a\\qb");
    }

    @Test
    void unescapeHandlesNullAndPlainText() {
        assertThat(EscapedLineReader.unescapeLine(null)).isNull();
        String plain = "no escapes";
        assertThat(EscapedLineReader.unescapeLine(plain)).isSameAs(plain);
    }

    @Test
    void rejectsMalformedUnicodeEscape() {
        assertThatThrownBy(() -> EscapedLineReader.unescapeLine("\\u00zz"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsProperties() throws IOException {
        try (EscapedLineReader r = reader("key = value\n\n  spaced key  =  spaced value  \nflag\na = b = c")) {
            assertThat(r.readProperty()).containsExactly("key", "value");
            assertThat(r.readProperty()).containsExactly("spaced key", "spaced value");
            assertThat(r.readProperty()).containsExactly("flag", "");
            assertThat(r.readProperty()).containsExactly("a", "b = c");
            assertThat(r.readProperty()).isNull();
        }
    }

    @Test
    void readPropertyCanKeepEmptyLines() throws IOException {
        try (EscapedLineReader r = reader("\nk = v")) {
            assertThat(r.readProperty(false)).containsExactly("", "");
            assertThat(r.readProperty(false)).containsExactly("k", "v");
        }
    }

    @Test
    void readsAllProperties() throws IOException {
        Map<String, String> props = new LinkedHashMap<>();
        try (EscapedLineReader r = reader("a = 1\n# c\nb = 2\na = 3")) {
            assertThat(r.readProperties(props)).isSameAs(props);
        }
        assertThat(props).containsExactly(Map.entry("a", "3"), Map.entry("b", "2"));
    }

    @Test
    void readNonemptyLineSkipsBlankLines() throws IOException {
        try (EscapedLineReader r = reader("\n  \n# c\nfound\\u0021\n")) {
            assertThat(r.readNonemptyLine()).isEqualTo("found!");
            assertThat(r.readNonemptyLine()).isNull();
        }
    }

    @Test
    void capturesFirstCommentBlock() throws IOException {
        try (EscapedLineReader r = reader("# first\n#second\nk = v\n# later")) {
            assertThat(r.getFirstCommentBlock()).isNull();
            r.readProperty();
            assertThat(r.getFirstCommentBlock()).isEqualTo("first\nsecond");
            r.readProperty();
            assertThat(r.getFirstCommentBlock()).isEqualTo("first\nsecond");
        }
    }

    @Test
    void tracksLineNumbersFromOne() throws IOException {
        try (EscapedLineReader r = reader("a\nb\\\nc\nd")) {
            assertThat(r.getLineNumber()).isEqualTo(1);
            r.readLine();
            assertThat(r.getLineNumber()).isEqualTo(2);
            r.readLine();
            assertThat(r.getLineNumber()).isEqualTo(4);
        }
    }

    @Test
    void defaultCharsetIsIso885915() throws IOException {
        byte[] euro = {'k', '=', (byte) 0xA4};
        try (EscapedLineReader r = new EscapedLineReader(new ByteArrayInputStream(euro))) {
            assertThat(r.readProperty()).containsExactly("k", "€");
        }
        File f = tmp.resolve("euro.settings").toFile();
        Files.write(f.toPath(), euro);
        try (EscapedLineReader r = new EscapedLineReader(f)) {
            assertThat(r.readProperty()).containsExactly("k", "€");
        }
        try (EscapedLineReader r = new EscapedLineReader(f.toURI().toURL())) {
            assertThat(r.readProperty()).containsExactly("k", "€");
        }
    }

    @Test
    void namedCharsetsAreHonored() throws IOException {
        byte[] utf8 = "k=€".getBytes(StandardCharsets.UTF_8);
        try (EscapedLineReader r = new EscapedLineReader(new ByteArrayInputStream(utf8), "UTF-8")) {
            assertThat(r.readProperty()).containsExactly("k", "€");
        }
        try (EscapedLineReader r = new EscapedLineReader(new ByteArrayInputStream(utf8), StandardCharsets.UTF_8)) {
            assertThat(r.readProperty()).containsExactly("k", "€");
        }
        Path f = tmp.resolve("utf8.txt");
        Files.write(f, utf8);
        try (EscapedLineReader r = new EscapedLineReader(f.toFile(), "UTF-8")) {
            assertThat(r.readProperty()).containsExactly("k", "€");
        }
        try (EscapedLineReader r = new EscapedLineReader(new StringReader("a"), 16)) {
            assertThat(r.readLine()).isEqualTo("a");
        }
    }
}
