package org.strangeeons.core.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EscapedLineWriterTest {

    @TempDir
    Path tmp;

    private static String write(boolean unicode, IOAction action) throws IOException {
        StringWriter out = new StringWriter();
        try (EscapedLineWriter w = new EscapedLineWriter(out)) {
            w.setUnicodeEscaped(unicode);
            action.run(w);
        }
        return out.toString();
    }

    private interface IOAction {
        void run(EscapedLineWriter w) throws IOException;
    }

    private static Map<String, String> read(String text) throws IOException {
        try (EscapedLineReader r = new EscapedLineReader(new StringReader(text))) {
            return r.readProperties(new LinkedHashMap<>());
        }
    }

    @Test
    void writesPropertiesOnSeparateLines() throws IOException {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("a", "1");
        props.put("b", "2");
        assertThat(write(true, w -> w.writeProperties(props))).isEqualTo("a = 1\nb = 2");
    }

    @Test
    void escapesValuesForTheReader() throws IOException {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("text", "line one\nline two\ttabbed");
        props.put("accent", "café ✓");
        String text = write(true, w -> w.writeProperties(props));
        assertThat(text).contains("\\n", "\\t", "\\u00e9", "\\u2713").startsWith("text = ");
        assertThat(read(text)).isEqualTo(props);
    }

    @Test
    void unicodeEscapesCanBeTurnedOff() throws IOException {
        StringWriter out = new StringWriter();
        try (EscapedLineWriter w = new EscapedLineWriter(out)) {
            assertThat(w.isUnicodeEscaped()).isTrue();
            w.setUnicodeEscaped(false);
            assertThat(w.isUnicodeEscaped()).isFalse();
            w.writeProperty("k", "café");
        }
        assertThat(out).hasToString("k = café");
    }

    @Test
    void wrapsLongLinesThatReadBackWhole() throws IOException {
        String value = "word ".repeat(60).trim();
        String text = write(true, w -> w.writeProperty("long", value));
        assertThat(text.lines().count()).isGreaterThan(1);
        assertThat(text.lines()).allSatisfy(line -> assertThat(line.length()).isLessThanOrEqualTo(90));
        assertThat(read(text)).containsExactly(Map.entry("long", value));
    }

    @Test
    void writesCommentsAndLines() throws IOException {
        String text = write(true, w -> {
            w.writeComment("one\ntwo");
            w.writeComment("three");
            w.writeLine("  # plain");
        });
        assertThat(text).isEqualTo("# one\n# two\n# three\n  \\# plain");
        try (EscapedLineReader r = new EscapedLineReader(new StringReader(text))) {
            r.readLine();
            assertThat(r.getFirstCommentBlock()).isEqualTo("one\ntwo\nthree");
        }
    }

    @Test
    void streamsUseTheirCharsets() throws IOException {
        ByteArrayOutputStream iso = new ByteArrayOutputStream();
        try (EscapedLineWriter w = new EscapedLineWriter(iso)) {
            w.setUnicodeEscaped(false);
            w.writeProperty("k", "€");
        }
        assertThat(iso.toByteArray()).containsExactly('k', ' ', '=', ' ', (byte) 0xA4);

        ByteArrayOutputStream utf8 = new ByteArrayOutputStream();
        try (EscapedLineWriter w = new EscapedLineWriter(utf8, "UTF-8")) {
            w.setUnicodeEscaped(false);
            w.writeProperty("k", "€");
        }
        assertThat(utf8.toString(StandardCharsets.UTF_8)).isEqualTo("k = €");
    }

    @Test
    void filesUseTheirCharsets() throws IOException {
        File iso = tmp.resolve("iso.settings").toFile();
        try (EscapedLineWriter w = new EscapedLineWriter(iso)) {
            w.writeProperty("k", "v");
        }
        assertThat(Files.readString(iso.toPath())).isEqualTo("k = v");

        File utf8 = tmp.resolve("utf8.settings").toFile();
        try (EscapedLineWriter w = new EscapedLineWriter(utf8, "UTF-8")) {
            w.setUnicodeEscaped(false);
            w.writeProperty("k", "€");
        }
        assertThat(Files.readString(utf8.toPath(), StandardCharsets.UTF_8)).isEqualTo("k = €");

        StringWriter sized = new StringWriter();
        try (EscapedLineWriter w = new EscapedLineWriter(sized, 16)) {
            w.writeLine("x");
        }
        assertThat(sized).hasToString("x");
    }
}
