package gamedata;

import org.strangeeons.core.io.EscapedLineReader;

/**
 * An exception that may be thrown when an error occurs while parsing a resource
 * file. This exception generally indicates that the file contains a syntax
 * error.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see ResourceParser
 */
@SuppressWarnings("serial")
public class ResourceParserException extends RuntimeException {

    private int line = 0;
    private String resource = null;

    public ResourceParserException() {
    }

    public ResourceParserException(String message) {
        super(message);
    }

    public ResourceParserException(String resource, String message) {
        super(message);
        this.resource = resource;
    }

    public ResourceParserException(String resource, String message, int line) {
        super(message);
        this.resource = resource;
        this.line = line;
    }

    public ResourceParserException(String resource, String message, EscapedLineReader reader) {
        super(message);
        this.resource = resource;
        this.line = reader == null ? -1 : reader.getLineNumber();
    }

    /**
     * Keeps binary compatibility with plug-ins compiled against the old reader.
     *
     * @deprecated Use {@link #ResourceParserException(String, String, EscapedLineReader)}.
     */
    @Deprecated(since = "3.6")
    public ResourceParserException(String resource, String message, ca.cgjennings.io.EscapedLineReader reader) {
        this(resource, message, (EscapedLineReader) reader);
    }

    /**
     * Returns the resource file that was being parsed, if known, or
     * {@code null}.
     *
     * @return the file containing the error
     */
    public String getResourceFile() {
        return resource;
    }

    /**
     * Returns the line number of the error, if known, or 0.
     *
     * @return the line numner where parsing failed
     */
    public int getLineNumber() {
        return line < 0 ? 0 : line;
    }

    @Override
    public String getLocalizedMessage() {
        String m = getMessage();
        if (getResourceFile() != null) {
            if (m == null) {
                m = "";
            }
            if (getLineNumber() > 0) {
                m += " (" + getMessage() + ":" + getLineNumber() + ")";
            } else {
                m += " (" + getMessage() + ")";
            }
        }
        return m;
    }
}
