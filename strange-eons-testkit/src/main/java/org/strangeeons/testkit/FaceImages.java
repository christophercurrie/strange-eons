package org.strangeeons.testkit;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.strangeeons.core.graphics.awt.AwtGraphics;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.ComponentHandler;
import org.strangeeons.plugins.graphics.ImageHandle;

/**
 * Paints a component handler's faces to images, as the application paints a
 * component's sheets for export: an opaque image the size of the face's
 * template at the requested resolution, with export rendering hints, painted
 * by the handler's {@code createPainter} and {@code paint}. Card finishes such
 * as bleed margins are not applied.
 *
 * <pre>
 * TestSettings settings = new TestSettings();
 * settings.addSettingsFrom("MyGame/card.settings");
 * new MyHandler().create(settings);
 * masters.verify("front", FaceImages.render(new MyHandler(), settings, 0));
 * </pre>
 */
public final class FaceImages {

    private FaceImages() {
    }

    /**
     * Creates the face a handler paints.
     *
     * @param handler the handler
     * @param settings the component settings
     * @param faceIndex the face to create
     * @return the face
     * @throws IllegalArgumentException if the handler has no such face
     */
    public static TestFace face(ComponentHandler handler, SettingsReadWrite settings, int faceIndex) {
        Objects.requireNonNull(handler, "handler");
        if (faceIndex < 0 || faceIndex >= handler.getFaceCount()) {
            throw new IllegalArgumentException("face does not exist: " + faceIndex);
        }
        return new TestFace(settings, handler.getTemplateKey(faceIndex), faceIndex);
    }

    /**
     * Paints a face at its template resolution.
     *
     * @param handler the handler
     * @param settings the component settings
     * @param faceIndex the face to paint
     * @return the painted face
     */
    public static BufferedImage render(ComponentHandler handler, SettingsReadWrite settings, int faceIndex) {
        return render(handler, settings, faceIndex, -1d);
    }

    /**
     * Paints a face at a given resolution.
     *
     * @param handler the handler
     * @param settings the component settings
     * @param faceIndex the face to paint
     * @param ppi the resolution, in pixels per inch, or a negative value for
     * the template resolution
     * @return the painted face
     */
    public static BufferedImage render(ComponentHandler handler, SettingsReadWrite settings, int faceIndex,
            double ppi) {
        TestFace face = face(handler, settings, faceIndex);
        double templatePpi = face.getTemplateResolution2D();
        double scale = ppi < 0d ? 1d : ppi / templatePpi;
        ImageHandle template = face.getTemplateImage();
        int width = Math.max(1, (int) (template.getWidth() * scale));
        int height = Math.max(1, (int) (template.getHeight() * scale));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        handler.createPainter(face);
        Graphics2D g = image.createGraphics();
        try {
            if (scale != 1d) {
                g.scale(scale, scale);
            }
            g.addRenderingHints(StandardRendering.EXPORT_HINTS);
            handler.paint(face, new AwtGraphics(g));
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Paints every face at its template resolution.
     *
     * @param handler the handler
     * @param settings the component settings
     * @return the painted faces, in face order
     */
    public static List<BufferedImage> renderAll(ComponentHandler handler, SettingsReadWrite settings) {
        List<BufferedImage> faces = new ArrayList<>();
        for (int i = 0; i < handler.getFaceCount(); ++i) {
            faces.add(render(handler, settings, i));
        }
        return faces;
    }
}
