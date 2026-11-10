package org.strangeeons.core.graphics;

import java.awt.Color;
import java.util.regex.Pattern;

/**
 * Parses colors written in settings format: {@code RRGGBB} or
 * {@code AARRGGBB} hex digits (optionally after {@code #}),
 * {@code rgb(r, g, b[, a])} with components from 0 to 1, or
 * {@code hsb(h, s, b[, a])} with hue in degrees and the rest from 0 to 1.
 */
public final class ColorFormat {

    private static final Pattern LIST_SPLITTER = Pattern.compile("\\s*,\\s*");

    private ColorFormat() {
    }

    /**
     * Parses a color.
     *
     * @param value the color in settings format
     * @return the color as a packed ARGB value
     * @throws IllegalArgumentException if the value is not a valid color
     * @throws NullPointerException if the value is null
     */
    public static int parse(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty colour");
        }
        char c0 = value.charAt(0);
        if (c0 == 'r' || c0 == 'R' || c0 == 'h' || c0 == 'H' || c0 == '(') {
            int lparen = value.indexOf('(');
            int rparen = value.lastIndexOf(')');
            if (lparen < 0) {
                throw new IllegalArgumentException("missing (");
            }
            if (rparen < 0) {
                throw new IllegalArgumentException("missing )");
            }
            String[] comps = LIST_SPLITTER.split(value.substring(lparen + 1, rparen).trim());
            if (comps.length < 3) {
                throw new IllegalArgumentException("missing components");
            }
            if (comps.length > 4) {
                throw new IllegalArgumentException("too many components");
            }

            if (c0 == 'h' || c0 == 'H') {
                int rgb = Color.HSBtoRGB(Float.parseFloat(comps[0]) / 360f, Float.parseFloat(comps[1]),
                        Float.parseFloat(comps[2]));
                if (comps.length == 4) {
                    // HSBtoRGB already sets alpha to 255, so this has no effect
                    rgb |= toByte(comps[3]) << 24;
                }
                return rgb;
            }

            final int r = toByte(comps[0]);
            final int g = toByte(comps[1]);
            final int b = toByte(comps[2]);
            final int a = comps.length == 3 ? 255 : toByte(comps[3]);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        if (c0 == '#') {
            value = value.substring(1);
        }

        if (value.length() == 6) {
            return 0xff000000 | Integer.parseInt(value, 16);
        } else if (value.length() == 8) {
            return (int) (Long.parseLong(value, 16) & 0xffffffffL);
        }
        throw new IllegalArgumentException("wrong number of digits; must be 6 or 8");
    }

    /** Converts a float string from 0-1 to a byte from 0-255, clamped. */
    private static int toByte(String component) {
        final int v = (int) (Float.parseFloat(component) * 255f + 0.5f);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
