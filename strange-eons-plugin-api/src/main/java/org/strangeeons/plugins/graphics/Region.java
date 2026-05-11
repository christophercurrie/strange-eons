package org.strangeeons.plugins.graphics;

/**
 * A rectangular region described in template-resolution pixel
 * coordinates. The canonical settings format is
 * {@code "x,y,width,height"} with integer values, matching the
 * {@code prefab-title-region = 14,17,212,40} convention used in
 * {@code .settings} files.
 *
 * @since 3.6
 */
public record Region(int x, int y, int width, int height) {

    /** Bean-style getter for {@link #x()}. */
    public int getX() { return x; }

    /** Bean-style getter for {@link #y()}. */
    public int getY() { return y; }

    /** Bean-style getter for {@link #width()}. */
    public int getWidth() { return width; }

    /** Bean-style getter for {@link #height()}. */
    public int getHeight() { return height; }

    /**
     * Returns the canonical settings-format string for this region:
     * {@code "x,y,width,height"}.
     */
    public String toSetting() {
        return x + "," + y + "," + width + "," + height;
    }

    /**
     * Parses a region from its canonical settings-format string.
     *
     * @throws IllegalArgumentException if the string is not a valid
     *     region value
     */
    public static Region parse(String settingValue) {
        if (settingValue == null) {
            throw new IllegalArgumentException("region value is null");
        }
        String[] parts = settingValue.split("\\s*,\\s*");
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "expected 4 comma-separated integers: " + settingValue);
        }
        try {
            return new Region(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "region value contains non-integer: " + settingValue, e);
        }
    }
}
