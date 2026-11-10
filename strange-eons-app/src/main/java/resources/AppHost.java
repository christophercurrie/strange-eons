package resources;

import ca.cgjennings.graphics.shapes.SVGVectorImage;
import ca.cgjennings.graphics.shapes.VectorImage;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.strangeeons.core.Host;

/**
 * Supplies core code with the application's image preparation settings, SVG
 * support, themed stand-in images, cloud fonts, script evaluation and game
 * language.
 */
final class AppHost implements Host {

    @Override
    public BufferedImage prepareImage(Image image) {
        return ResourceKit.prepareNewImage(image);
    }

    @Override
    public VectorImage loadVectorImage(URL url) throws IOException {
        try {
            return new SVGVectorImage(url);
        } catch (CoreComponents.MissingCoreComponentException mcc) {
            // SVG support library not installed
            return null;
        }
    }

    @Override
    public VectorImage getMissingVectorImage() {
        return ResourceKit.getMissingVectorImage();
    }

    @Override
    public BufferedImage getMissingImage() {
        return ResourceKit.getMissingImage();
    }

    @Override
    public String resolveFontFamily(String family) {
        return ResourceKit.normalizeFontFamilyName(family);
    }

    /**
     * Evaluates a style value through the registered JavaScript language,
     * caching the result by source text. Opening a style-heavy deck evaluates
     * the same few literals thousands of times. Rhino {@code Scriptable}
     * results are not cached, since they keep their script scope alive.
     */
    @Override
    public Object evaluateStyleLiteral(String code) {
        Object cached = STYLE_LITERAL_CACHE.get(code);
        if (cached != null) {
            return cached;
        }
        Object result = Host.super.evaluateStyleLiteral(code);
        if (result != null && !(result instanceof org.mozilla.javascript.Scriptable)) {
            STYLE_LITERAL_CACHE.put(code, result);
        }
        return result;
    }

    private static final ConcurrentMap<String, Object> STYLE_LITERAL_CACHE = new ConcurrentHashMap<>();

    @Override
    public Locale getGameLocale() {
        Locale loc = Language.getGameLocale();
        return loc == null ? Locale.getDefault() : loc;
    }
}
