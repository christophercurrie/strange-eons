package ca.cgjennings.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Reads escaped lines and properties.
 *
 * @deprecated Use {@link org.strangeeons.core.io.EscapedLineReader}.
 */
@Deprecated(since = "3.6")
public class EscapedLineReader extends org.strangeeons.core.io.EscapedLineReader {

    public EscapedLineReader(Reader in) {
        super(in);
    }

    public EscapedLineReader(Reader in, int bufferSize) {
        super(in, bufferSize);
    }

    public EscapedLineReader(InputStream in) {
        super(in);
    }

    public EscapedLineReader(InputStream in, String charset) throws UnsupportedEncodingException {
        super(in, charset);
    }

    public EscapedLineReader(InputStream in, Charset charset) {
        super(in, charset);
    }

    public EscapedLineReader(URL url) throws IOException {
        super(url);
    }

    public EscapedLineReader(URL url, String charset) throws IOException {
        super(url, charset);
    }

    public EscapedLineReader(File f) throws IOException {
        super(f);
    }

    public EscapedLineReader(File f, String charset) throws IOException {
        super(f, charset);
    }
}
