package org.strangeeons.core.settings;

import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.layout.PageShape;
import ca.cgjennings.layout.TextStyle;
import java.awt.Color;
import java.awt.font.TextAttribute;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.strangeeons.core.Host;
import org.strangeeons.core.graphics.ColorFormat;

/**
 * Parses setting values. Each method throws {@link IllegalArgumentException}
 * (or {@link NullPointerException} for a missing value) when a value cannot be
 * parsed.
 */
public final class SettingValues {

    private static final Pattern LIST_SPLITTER = Pattern.compile("\\s*,\\s*");
    private static final Pattern INT_MATCHER = Pattern.compile("[+-]?[0-9]+");
    private static final Pattern FLOAT_MATCHER = Pattern.compile("[+-]?[0-9]*\\.[0-9]+");
    private static final Pattern COLOUR_MATCHER = Pattern.compile("\\#|((rgba?)\\()|(hsba?\\()", Pattern.CASE_INSENSITIVE);

    private SettingValues() {
    }

    /**
     * Returns whether a value means yes: {@code yes}, {@code true} or
     * {@code 1}, ignoring case and surrounding space. Anything else, including
     * {@code null}, means no.
     *
     * @param value the value to interpret
     * @return true if the value means yes
     */
    public static boolean yesNo(String value) {
        boolean answer = false;
        if (value != null) {
            value = value.trim();
            if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true") || value.equals("1")) {
                answer = true;
            }
        }
        return answer;
    }

    /**
     * Parses an integer in decimal, hex ({@code 0x}) or octal notation. A
     * decimal fraction is truncated.
     *
     * @param value the value to parse
     * @return the integer
     */
    public static int integer(String value) {
        value = value.trim();
        try {
            return Integer.decode(value);
        } catch (NumberFormatException e) {
            int decimal = value.indexOf('.');
            if (decimal == 0) {
                return 0;
            }
            if (decimal > 0) {
                return Integer.decode(value.substring(0, decimal));
            }
            throw e;
        }
    }

    /**
     * Parses a decimal number.
     *
     * @param value the value to parse
     * @return the number
     */
    public static double number(String value) {
        return Double.parseDouble(value.trim());
    }

    /**
     * Parses a cup shape from five comma-separated numbers.
     *
     * @param value the value to parse
     * @return the shape
     */
    public static PageShape.CupShape cupShape(String value) {
        String[] comps = LIST_SPLITTER.split(value.trim());
        if (comps.length != 5) {
            throw new IllegalArgumentException("need 5 values");
        }
        return new PageShape.CupShape(
                Double.parseDouble(comps[0]),
                Double.parseDouble(comps[1]),
                Double.parseDouble(comps[2]),
                Double.parseDouble(comps[3]),
                Double.parseDouble(comps[4])
        );
    }

    /**
     * Parses text alignment from comma-separated words: {@code left},
     * {@code right}, {@code center}, {@code top}, {@code bottom},
     * {@code middle}, {@code justified}, {@code unjustified}. The default is
     * top left, unjustified.
     *
     * @param value the value to parse; {@code null} gives the default
     * @return a combination of {@link MarkupRenderer} layout flags
     */
    public static int textAlignment(String value) {
        int valign = MarkupRenderer.LAYOUT_TOP;
        int halign = MarkupRenderer.LAYOUT_LEFT;
        int justify = 0;
        if (value != null) {
            String[] tokens = LIST_SPLITTER.split(value.trim());
            for (String token : tokens) {
                if (token.isEmpty()) {
                    continue;
                }
                if (token.equalsIgnoreCase("left")) {
                    halign = MarkupRenderer.LAYOUT_LEFT;
                } else if (token.equalsIgnoreCase("right")) {
                    halign = MarkupRenderer.LAYOUT_RIGHT;
                } else if (token.equalsIgnoreCase("center") || token.equalsIgnoreCase("centre")) {
                    halign = MarkupRenderer.LAYOUT_CENTER;
                } else if (token.equalsIgnoreCase("top")) {
                    valign = MarkupRenderer.LAYOUT_TOP;
                } else if (token.equalsIgnoreCase("bottom")) {
                    valign = MarkupRenderer.LAYOUT_BOTTOM;
                } else if (token.equalsIgnoreCase("middle")) {
                    valign = MarkupRenderer.LAYOUT_MIDDLE;
                } else if (token.equalsIgnoreCase("justified")) {
                    justify = MarkupRenderer.LAYOUT_JUSTIFY;
                } else if (token.equalsIgnoreCase("unjustified") || token.equalsIgnoreCase("ragged")) {
                    justify = 0;
                } else {
                    throw new IllegalArgumentException("unknown token: " + token);
                }
            }
        }
        return valign | halign | justify;
    }

    /**
     * Parses a text style of the form {@code ATTRIBUTE: value; ...}, adding
     * each attribute to {@code base}. Attribute and constant names are those
     * of {@link TextAttribute}. Colors become {@link Color} instances.
     *
     * @param value the style to parse
     * @param base the style to add to, or {@code null} for a new style
     * @return the style with the parsed attributes added
     */
    public static TextStyle textStyle(String value, TextStyle base) {
        return textStyle(value, base, v -> new Color(ColorFormat.parse(v), true));
    }

    /**
     * Parses a text style, converting color values with the given function.
     * Values in braces are evaluated by the {@linkplain Host#evaluateStyleLiteral
     * host}.
     *
     * @param value the style to parse
     * @param base the style to add to, or {@code null} for a new style
     * @param colours converts a color value in settings format to a color
     * @return the style with the parsed attributes added
     */
    public static TextStyle textStyle(String value, TextStyle base, Function<String, ? extends Color> colours) {
        if (base == null) {
            base = new TextStyle();
        }
        int s = 0;
        while (s < value.length()) {
            s = nextStyle(base, value, s, colours);
        }
        return base;
    }

    private static int nextStyle(TextStyle base, String styleDesc, int s, Function<String, ? extends Color> colours) {
        // Format is KEY1: VALUE1; KEY2: VALUE2; ...
        int attr = styleDesc.indexOf(':', s);
        if (attr >= 0) {
            String keyName = cleanStyleKey(styleDesc.substring(s, attr).trim());
            Object key = styleLookup(null, keyName);
            if (!(key instanceof TextAttribute)) {
                throw new IllegalArgumentException("token is not a TextAttribute: \"" + keyName + '"');
            }

            int token = findValueEnd(styleDesc, attr + 1);
            String valueText = styleDesc.substring(attr + 1, token).trim();

            // remove literal markers if present; if literal markers are present,
            // then the value is that object and no additional parsing will be done
            Object value = null;
            boolean isLiteral = false;
            if (!valueText.isEmpty()) {
                char start = valueText.charAt(0);
                char end = valueText.length() > 1 ? valueText.charAt(valueText.length() - 1) : '\0';
                switch (start) {
                    case '{':
                        isLiteral = true;
                        valueText = valueText.substring(1, valueText.length() - (end == '}' ? 1 : 0));
                        value = Host.get().evaluateStyleLiteral(valueText);
                        break;
                    case '\'':
                    case '"':
                        isLiteral = true;
                        value = valueText.substring(1, valueText.length() - (end == start ? 1 : 0));
                        break;
                }
            }

            // if it wasn't a string literal or code literal, try to parse
            // other object types out, including TextAttribute constants
            if (!isLiteral) {
                if (valueText.isEmpty()) {
                    value = null;
                } else if (COLOUR_MATCHER.matcher(valueText).find()) {
                    value = colours.apply(valueText);
                } else if (INT_MATCHER.matcher(valueText).matches()) {
                    value = Integer.valueOf(valueText);
                } else if (FLOAT_MATCHER.matcher(valueText).matches()) {
                    value = Float.valueOf(valueText);
                } else {
                    valueText = cleanStyleKey(valueText);
                    value = styleLookup(keyName, valueText);
                    if (value instanceof TextAttribute) {
                        throw new IllegalArgumentException("expected value but found TextAttribute: \"" + valueText + '"');
                    }
                }
            }

            base.add((TextAttribute) key, value);

            return token + 1;
        }
        return styleDesc.length();
    }

    @SuppressWarnings("empty-statement")
    private static int findValueEnd(String styleDesc, int start) {
        int i = start;

        // skip past initial whitespace, if any
        for (; i < styleDesc.length() && Character.isSpaceChar(styleDesc.charAt(i)); ++i);

        // nothing but spaces
        if (i >= styleDesc.length()) {
            return styleDesc.length();
        }

        // if the first char is {, ', or " then this is the start of a string;
        // otherwise we simply find the terminating ; and we are done
        char stackOpen = styleDesc.charAt(i);
        char stackClose;
        switch (stackOpen) {
            case '{':
                stackClose = '}';
                break;
            case '\'':
            case '"':
                stackClose = stackOpen;
                break;
            default:
                int semi = styleDesc.indexOf(';', i);
                return semi < 0 ? styleDesc.length() : semi;
        }

        ++i;
        int stackCount = 1;
        for (; i < styleDesc.length(); ++i) {
            char c = styleDesc.charAt(i);
            if (c == stackOpen && stackOpen != stackClose) {
                ++stackCount;
            } else if (c == stackClose) {
                if (--stackCount < 0) {
                    throw new IllegalArgumentException(stackClose + " without " + stackOpen + " at " + (i + 1));
                }
            } else if (c == ';' && stackCount == 0) {
                return i;
            }
        }

        if (stackCount > 0) {
            throw new IllegalArgumentException("unclosed " + stackOpen + " literal at " + (i + 1));
        }
        return styleDesc.length();
    }

    private static String cleanStyleKey(String key) {
        return key.toUpperCase(Locale.CANADA).replace(' ', '_');
    }

    private static Object styleLookup(String base, String name) {
        if (STYLE_MAP.containsKey(name)) {
            return STYLE_MAP.get(name);
        }
        if (base != null) {
            base = base + '_' + name;
            if (STYLE_MAP.containsKey(base)) {
                return STYLE_MAP.get(base);
            }
        }
        throw new IllegalArgumentException("unknown token: " + base);
    }

    private static final HashMap<String, Object> STYLE_MAP = new HashMap<>(100);

    static {
        try {
            for (Field f : TextAttribute.class.getFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                STYLE_MAP.put(f.getName(), f.get(null));
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
        STYLE_MAP.put("JUSTIFICATION_OFF", 0f);
        STYLE_MAP.put("KERNING_OFF", 0);
        STYLE_MAP.put("LIGATURES_OFF", 0);
        STYLE_MAP.put("POSTURE_ITALIC", TextAttribute.POSTURE_OBLIQUE);
        STYLE_MAP.put("STRIKETHROUGH_OFF", false);
        STYLE_MAP.put("SWAP_COLORS_OFF", false);
        STYLE_MAP.put("SWAP_COLOURS", TextAttribute.SWAP_COLORS);
        STYLE_MAP.put("SWAP_COLOURS_ON", true);
        STYLE_MAP.put("SWAP_COLOURS_OFF", false);
        STYLE_MAP.put("TRACKING_DEFAULT", 0f);
        STYLE_MAP.put("UNDERLINE_OFF", -1);
        STYLE_MAP.put("WIDTH_SEMICONDENSED", TextAttribute.WIDTH_SEMI_CONDENSED);
        STYLE_MAP.put("WIDTH_SEMIEXTENDED", TextAttribute.WIDTH_SEMI_EXTENDED);
    }
}
