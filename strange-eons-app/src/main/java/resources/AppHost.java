package resources;

import ca.cgjennings.graphics.shapes.SVGVectorImage;
import ca.cgjennings.graphics.shapes.VectorImage;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
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

    @Override
    public Object evaluateStyleLiteral(String code) {
        return Settings.evaluateStyleLiteral(code);
    }

    @Override
    public Locale getGameLocale() {
        Locale loc = Language.getGameLocale();
        return loc == null ? Locale.getDefault() : loc;
    }
}
