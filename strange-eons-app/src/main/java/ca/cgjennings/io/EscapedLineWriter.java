package ca.cgjennings.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Writes escaped lines and properties.
 *
 * @deprecated Use {@link org.strangeeons.core.io.EscapedLineWriter}.
 */
@Deprecated(since = "3.6")
public class EscapedLineWriter extends org.strangeeons.core.io.EscapedLineWriter {

    public EscapedLineWriter(File f) throws IOException {
        super(f);
    }

    public EscapedLineWriter(File f, String charset) throws IOException {
        super(f, charset);
    }

    public EscapedLineWriter(OutputStream out) throws IOException {
        super(out);
    }

    public EscapedLineWriter(OutputStream out, String charset) throws IOException {
        super(out, charset);
    }

    public EscapedLineWriter(Writer out, int sz) {
        super(out, sz);
    }

    public EscapedLineWriter(Writer out) {
        super(out);
    }
}
