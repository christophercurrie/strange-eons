package ca.cgjennings.text;

/**
 * Wraps long lines of text.
 *
 * @deprecated Use {@link org.strangeeons.core.text.LineWrapper}.
 */
@Deprecated(since = "3.6")
public class LineWrapper extends org.strangeeons.core.text.LineWrapper {

    public LineWrapper() {
    }

    public LineWrapper(String breakText, int firstWrap, int afterWrap, int tolerance) {
        super(breakText, firstWrap, afterWrap, tolerance);
    }
}
