package ca.cgjennings.layout;

import java.awt.font.TextAttribute;

import org.strangeeons.core.Host;

/**
 * A parametric style that changes the font family.
 *
 * @author Chris Jennings {@literal <https://cgjennings.ca/contact>}
 */
public class FontFamilyStyleFactory implements ParametricStyleFactory {

    @Override
    public TextStyle createStyle(MarkupRenderer renderer, String[] parameters) {
        String family = "";
        if (parameters.length >= 1) {
            family = parameters[0];
        }
        family = Host.get().resolveFontFamily(family);
        return new TextStyle(TextAttribute.FAMILY, family);
    }

    public static final FontFamilyStyleFactory getShared() {
        return shared;
    }

    private static final FontFamilyStyleFactory shared = new FontFamilyStyleFactory();
}
