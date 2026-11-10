package org.strangeeons.core;

import ca.cgjennings.graphics.shapes.VectorImage;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import org.strangeeons.core.graphics.ImageFormats;
import org.strangeeons.core.graphics.MissingImages;
import org.strangeeons.core.scripts.ScriptLanguages;
import org.strangeeons.scripts.ScriptException;
import org.strangeeons.scripts.ScriptLanguage;
import org.strangeeons.scripts.ScriptSession;

/**
 * Services that core code obtains from the application. Every method has a
 * default that works without the application, so core code runs on its own
 * in tests; the application {@linkplain #set installs} a host that supplies
 * its settings, installed libraries and current language.
 */
public interface Host {

    /**
     * Returns the installed host.
     *
     * @return the current host
     */
    static Host get() {
        return HostHolder.current;
    }

    /**
     * Installs a host.
     *
     * @param host the host to install
     */
    static void set(Host host) {
        HostHolder.current = Objects.requireNonNull(host, "host");
    }

    /**
     * Converts a newly loaded image to the format used for drawing. The
     * default converts it to an integer RGB or ARGB format.
     *
     * @param image the image to prepare, or {@code null}
     * @return the prepared image, or {@code null} if {@code image} is null
     */
    default BufferedImage prepareImage(Image image) {
        return image == null ? null : ImageFormats.ensureIntRGBFormat(ImageFormats.imageToBufferedImage(image));
    }

    /**
     * Loads a vector image, such as an SVG file. The default supports no
     * vector formats.
     *
     * @param url the location of the image
     * @return the image, or {@code null} if vector images are not supported
     * @throws IOException if the image cannot be read
     */
    default VectorImage loadVectorImage(URL url) throws IOException {
        return null;
    }

    /**
     * Returns the vector image drawn in place of an image that cannot be
     * loaded.
     *
     * @return the stand-in image
     */
    default VectorImage getMissingVectorImage() {
        return HostHolder.MISSING;
    }

    /**
     * Returns the bitmap drawn in place of an image that cannot be loaded.
     *
     * @return the stand-in image
     */
    default BufferedImage getMissingImage() {
        VectorImage vi = getMissingVectorImage();
        return vi == HostHolder.MISSING ? HostHolder.missingRaster() : vi.createRasterImage(64, 64, true);
    }

    /**
     * Converts a font family name from a setting or markup to one that can be
     * used as a text attribute. The default trims it and drops a
     * {@code cloud:} prefix.
     *
     * @param family the family name, or {@code null}
     * @return the usable family name; empty if {@code family} is null
     */
    default String resolveFontFamily(String family) {
        if (family == null) {
            return "";
        }
        family = family.trim();
        return family.startsWith("cloud:") ? family.substring(6).trim() : family;
    }

    /**
     * Evaluates the script code in a braced text style value, such as
     * {@code FOREGROUND: {new Color(0, 0, 0)}}. The default runs it in a new
     * session of the registered {@value ScriptLanguages#JAVASCRIPT} language,
     * after importing the packages style values may refer to.
     *
     * @param code the code between the braces
     * @return the value of the code
     * @throws UnsupportedOperationException if no JavaScript language is
     * registered
     * @throws IllegalArgumentException if the code fails
     */
    default Object evaluateStyleLiteral(String code) {
        ScriptLanguage js = ScriptLanguages.find(ScriptLanguages.JAVASCRIPT);
        if (js == null) {
            throw new UnsupportedOperationException(
                    "script values in text styles need a JavaScript language: {" + code + "}");
        }
        try (ScriptSession session = js.createSession()) {
            session.eval(HostHolder.STYLE_LITERAL_IMPORTS, "style setting imports");
            return session.eval(code, "style setting literal");
        } catch (ScriptException e) {
            throw new IllegalArgumentException("script value failed: {" + code + "}", e);
        }
    }

    /**
     * Returns the locale of game component text. The default is the JVM's
     * default locale.
     *
     * @return the game locale
     */
    default Locale getGameLocale() {
        return Locale.getDefault();
    }
}

/** Holds the installed host. */
final class HostHolder {

    static final String STYLE_LITERAL_IMPORTS
            = "importPackage(gamedata);"
            + "importPackage(resources);"
            + "importClass(java.awt.font.TextAttribute);"
            + "importClass(java.awt.font.TransformAttribute);"
            + "importClass(java.awt.geom.AffineTransform);";

    static final VectorImage MISSING = MissingImages.createDefault();
    static volatile Host current = new Host() {
    };
    private static BufferedImage missingRaster;

    static synchronized BufferedImage missingRaster() {
        if (missingRaster == null) {
            missingRaster = MISSING.createRasterImage(64, 64, true);
        }
        return missingRaster;
    }

    private HostHolder() {
    }
}
