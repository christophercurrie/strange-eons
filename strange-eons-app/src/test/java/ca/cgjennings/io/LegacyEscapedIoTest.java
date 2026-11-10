package ca.cgjennings.io;

import static org.assertj.core.api.Assertions.assertThat;

import ca.cgjennings.text.LineWrapper;
import gamedata.ResourceParserException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("deprecation")
class LegacyEscapedIoTest {

    @TempDir
    Path tmp;

    @Test
    void oldReaderNamesStillRead() throws IOException {
        byte[] bytes = "k = v".getBytes(StandardCharsets.ISO_8859_1);
        File f = tmp.resolve("a.settings").toFile();
        Files.write(f.toPath(), bytes);
        EscapedLineReader[] readers = {
            new EscapedLineReader(new StringReader("k = v")),
            new EscapedLineReader(new StringReader("k = v"), 16),
            new EscapedLineReader(new ByteArrayInputStream(bytes)),
            new EscapedLineReader(new ByteArrayInputStream(bytes), "UTF-8"),
            new EscapedLineReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8),
            new EscapedLineReader(f.toURI().toURL()),
            new EscapedLineReader(f.toURI().toURL(), "UTF-8"),
            new EscapedLineReader(f),
            new EscapedLineReader(f, "UTF-8"),
        };
        for (EscapedLineReader r : readers) {
            try (r) {
                assertThat(r).isInstanceOf(org.strangeeons.core.io.EscapedLineReader.class);
                assertThat(r.readProperty()).containsExactly("k", "v");
            }
        }
        assertThat(EscapedLineReader.unescapeLine("\\n")).isEqualTo("\n");
    }

    @Test
    void oldWriterNamesStillWrite() throws IOException {
        File f = tmp.resolve("b.settings").toFile();
        StringWriter sw = new StringWriter();
        EscapedLineWriter[] writers = {
            new EscapedLineWriter(f),
            new EscapedLineWriter(tmp.resolve("c.settings").toFile(), "UTF-8"),
            new EscapedLineWriter(new ByteArrayOutputStream()),
            new EscapedLineWriter(new ByteArrayOutputStream(), "UTF-8"),
            new EscapedLineWriter(new StringWriter(), 16),
            new EscapedLineWriter(sw),
        };
        for (EscapedLineWriter w : writers) {
            try (w) {
                assertThat(w).isInstanceOf(org.strangeeons.core.io.EscapedLineWriter.class);
                w.writeProperty("k", "v");
            }
        }
        assertThat(sw).hasToString("k = v");
        assertThat(Files.readString(f.toPath())).isEqualTo("k = v");
    }

    @Test
    void oldCodecDelegates() {
        int flags = EscapedTextCodec.UNICODE | EscapedTextCodec.SPACE | EscapedTextCodec.USE_S_ESCAPE
                | EscapedTextCodec.ASSIGNMENT | EscapedTextCodec.INITIAL_COMMENT | EscapedTextCodec.WHITESPACE
                | EscapedTextCodec.BACKSLASH;
        assertThat(flags).isEqualTo(0x7f);
        assertThat(EscapedTextCodec.isWrappedLine("a\\")).isTrue();
        assertThat(EscapedTextCodec.escapeUnicode("é")).isEqualTo("\\u00e9");
        assertThat(EscapedTextCodec.escape("a b", EscapedTextCodec.SPACE)).isEqualTo("a\\ b");
        assertThat(EscapedTextCodec.unescapeUnicode("\\u00e9")).isEqualTo("é");
        assertThat(EscapedTextCodec.unescape("\\t")).isEqualTo("\t");
    }

    @Test
    void oldLineWrapperWraps() {
        assertThat(new LineWrapper()).isInstanceOf(org.strangeeons.core.text.LineWrapper.class);
        assertThat(new LineWrapper("|", 4, 4, 0).wrap("aaa bbb")).isEqualTo("aaa |bbb");
    }

    @Test
    void parserExceptionTakesEitherReader() throws IOException {
        try (EscapedLineReader old = new EscapedLineReader(new StringReader("a\nb"));
                org.strangeeons.core.io.EscapedLineReader core = new org.strangeeons.core.io.EscapedLineReader(new StringReader("a"))) {
            old.readLine();
            assertThat(new ResourceParserException("r", "m", old).getLineNumber()).isEqualTo(2);
            assertThat(new ResourceParserException("r", "m", core).getLineNumber()).isEqualTo(1);
            assertThat(new ResourceParserException("r", "m", (org.strangeeons.core.io.EscapedLineReader) null)
                    .getLineNumber()).isZero();
        }
    }
}
