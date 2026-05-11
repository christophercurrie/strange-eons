package org.strangeeons.plugins.graphics;

/**
 * A font typeface plus a size and a style flag. The canonical
 * settings format is {@code "family,size,style"} where {@code style}
 * is one of {@code plain}, {@code bold}, {@code italic}, or
 * {@code bolditalic}.
 *
 * @since 3.6
 */
public record Font(String family, double size, Style style) {

    /** A font style flag covering the SE settings-format vocabulary. */
    public enum Style {
        REGULAR, BOLD, ITALIC, BOLD_ITALIC;
    }

    /** Bean-style getter for {@link #family()}. */
    public String getFamily() { return family; }

    /** Bean-style getter for {@link #size()}. */
    public double getSize() { return size; }

    /** Bean-style getter for {@link #style()}. */
    public Style getStyle() { return style; }

    /** Creates a regular-style font at the given size. */
    public static Font of(String family, double size) {
        return new Font(family, size, Style.REGULAR);
    }

    /**
     * Returns the canonical settings-format string for this font:
     * {@code "family,size,style"}.
     */
    public String toSetting() {
        return family + "," + size + "," + styleToken();
    }

    /**
     * Parses a font from its canonical settings-format string.
     *
     * @throws IllegalArgumentException if the string is not a valid
     *     font value
     */
    public static Font parse(String settingValue) {
        if (settingValue == null) {
            throw new IllegalArgumentException("font value is null");
        }
        String[] parts = settingValue.split("\\s*,\\s*");
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException(
                    "expected family,size[,style]: " + settingValue);
        }
        String family = parts[0].trim();
        double size;
        try {
            size = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "font size is not numeric: " + settingValue, e);
        }
        Style style = Style.REGULAR;
        if (parts.length == 3) {
            style = parseStyle(parts[2].trim());
        }
        return new Font(family, size, style);
    }

    private static Style parseStyle(String s) {
        return switch (s.toLowerCase()) {
            case "", "plain", "regular" -> Style.REGULAR;
            case "bold" -> Style.BOLD;
            case "italic" -> Style.ITALIC;
            case "bolditalic", "bold-italic", "bold_italic" -> Style.BOLD_ITALIC;
            default -> throw new IllegalArgumentException(
                    "unrecognized font style: " + s);
        };
    }

    private String styleToken() {
        return switch (style) {
            case REGULAR -> "plain";
            case BOLD -> "bold";
            case ITALIC -> "italic";
            case BOLD_ITALIC -> "bolditalic";
        };
    }
}
