package ca.cgjennings.io;

/**
 * Escapes and unescapes text.
 *
 * @deprecated Use {@link org.strangeeons.core.io.EscapedTextCodec}.
 */
@Deprecated(since = "3.6")
public final class EscapedTextCodec {

    private EscapedTextCodec() {
    }

    public static final int UNICODE = org.strangeeons.core.io.EscapedTextCodec.UNICODE;
    public static final int SPACE = org.strangeeons.core.io.EscapedTextCodec.SPACE;
    public static final int USE_S_ESCAPE = org.strangeeons.core.io.EscapedTextCodec.USE_S_ESCAPE;
    public static final int ASSIGNMENT = org.strangeeons.core.io.EscapedTextCodec.ASSIGNMENT;
    public static final int INITIAL_COMMENT = org.strangeeons.core.io.EscapedTextCodec.INITIAL_COMMENT;
    public static final int WHITESPACE = org.strangeeons.core.io.EscapedTextCodec.WHITESPACE;
    public static final int BACKSLASH = org.strangeeons.core.io.EscapedTextCodec.BACKSLASH;

    public static boolean isWrappedLine(CharSequence s) {
        return org.strangeeons.core.io.EscapedTextCodec.isWrappedLine(s);
    }

    public static String escapeUnicode(CharSequence s) {
        return org.strangeeons.core.io.EscapedTextCodec.escapeUnicode(s);
    }

    public static String escape(CharSequence s, int escapeFlags) {
        return org.strangeeons.core.io.EscapedTextCodec.escape(s, escapeFlags);
    }

    public static String unescapeUnicode(CharSequence s) {
        return org.strangeeons.core.io.EscapedTextCodec.unescapeUnicode(s);
    }

    public static String unescape(CharSequence s) {
        return org.strangeeons.core.io.EscapedTextCodec.unescape(s);
    }
}
