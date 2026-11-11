package org.strangeeons.testkit;

import java.util.Objects;
import org.strangeeons.core.Host;
import org.strangeeons.core.graphics.awt.AwtImageHandle;
import org.strangeeons.core.graphics.awt.AwtMarkupRenderer;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.Face;
import org.strangeeons.plugins.graphics.ImageHandle;
import org.strangeeons.plugins.graphics.MarkupRenderer;
import org.strangeeons.plugins.graphics.Region;

/**
 * A face of a component for painting without the application. Keys are read
 * as the application reads them for a component sheet: the template image
 * from <i>key</i>{@code -template}, its resolution from <i>key</i>{@code -ppi}
 * (or {@code -dpi}, default 150), regions from
 * <i>key</i>{@code -}<i>name</i>{@code -region}, and images from
 * <i>key</i>{@code -}<i>name</i>. A missing template or image is replaced by
 * the host's stand-in image.
 */
public final class TestFace implements Face {

    /** The template key the application uses when a handler names none. */
    public static final String DEFAULT_TEMPLATE_KEY = "diy-front-sheet";

    private static final double DEFAULT_PPI = 150d;

    private final SettingsReadWrite settings;
    private final String templateKey;
    private final int faceIndex;

    /**
     * Creates a face.
     *
     * @param settings the component settings
     * @param templateKey the face's template key, without {@code -template};
     * {@code null} for the default
     * @param faceIndex the face index
     */
    public TestFace(SettingsReadWrite settings, String templateKey, int faceIndex) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.templateKey = templateKey == null ? DEFAULT_TEMPLATE_KEY : templateKey;
        if (faceIndex < 0) {
            throw new IllegalArgumentException("faceIndex: " + faceIndex);
        }
        this.faceIndex = faceIndex;
    }

    @Override
    public int getFaceIndex() {
        return faceIndex;
    }

    @Override
    public String getTemplateKey() {
        return templateKey;
    }

    @Override
    public SettingsReadWrite getSettings() {
        return settings;
    }

    /**
     * Returns the template resolution exactly, as the application uses it for
     * layout; {@link #getTemplateResolution} rounds it.
     *
     * @return the template resolution, in pixels per inch
     */
    public double getTemplateResolution2D() {
        if (settings.get(templateKey + "-ppi") != null) {
            return settings.getDouble(templateKey + "-ppi");
        }
        if (settings.get(templateKey + "-dpi") != null) {
            return settings.getDouble(templateKey + "-dpi");
        }
        return DEFAULT_PPI;
    }

    @Override
    public int getTemplateResolution() {
        return (int) Math.round(getTemplateResolution2D());
    }

    @Override
    public Region region(String key) {
        return settings.readRegion(templateKey + '-' + key + "-region");
    }

    @Override
    public ImageHandle image(String key) {
        return imageOrStandIn(templateKey + '-' + key);
    }

    @Override
    public ImageHandle getTemplateImage() {
        return imageOrStandIn(templateKey + "-template");
    }

    /**
     * Creates a markup renderer at the template resolution, set up as the
     * application sets up renderers for sheets.
     */
    @Override
    public MarkupRenderer createMarkupRenderer() {
        ca.cgjennings.layout.MarkupRenderer r = new ca.cgjennings.layout.MarkupRenderer(getTemplateResolution2D());
        StandardRendering.initialize(r, settings);
        return new AwtMarkupRenderer(r);
    }

    private ImageHandle imageOrStandIn(String key) {
        ImageHandle image = settings.get(key) == null ? null : settings.readImage(key);
        return image != null ? image : new AwtImageHandle(Host.get().getMissingImage(), key);
    }
}
