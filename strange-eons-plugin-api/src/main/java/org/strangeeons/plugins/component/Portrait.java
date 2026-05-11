package org.strangeeons.plugins.component;

import org.strangeeons.plugins.graphics.ImageHandle;
import org.strangeeons.plugins.graphics.Region;

/**
 * State for a single portrait slot on a component: the image
 * handle, its positioning (pan, scale, rotation), and the clipping
 * region that masks how it appears on the rendered face.
 *
 * <p>A component may expose zero or more portraits via
 * {@link PortraitProvider}; each portrait is independently
 * positioned and clipped. The image and its source identifier are
 * carried by the {@link ImageHandle} — plug-ins set a new portrait
 * image via {@link #setImageHandle(ImageHandle)} and the handle
 * itself reports its source through
 * {@link ImageHandle#getSource()}.
 *
 * @since 3.6
 */
public interface Portrait {

    /** Returns the current image handle for this portrait. */
    ImageHandle getImageHandle();

    /**
     * Replaces the portrait's image with the given handle. The
     * handle's {@link ImageHandle#getSource()} becomes the source
     * identifier reported by this portrait.
     */
    void setImageHandle(ImageHandle handle);

    /** Returns the horizontal pan offset in template-resolution pixels. */
    double getPanX();

    /** Sets the horizontal pan offset. */
    void setPanX(double x);

    /** Returns the vertical pan offset in template-resolution pixels. */
    double getPanY();

    /** Sets the vertical pan offset. */
    void setPanY(double y);

    /** Returns the scale factor applied to the image. */
    double getScale();

    /** Sets the scale factor applied to the image. */
    void setScale(double scale);

    /** Returns the rotation in degrees. */
    double getRotation();

    /** Sets the rotation in degrees. */
    void setRotation(double degrees);

    /**
     * Returns the clipping region that masks the rendered portrait.
     * May be {@code null} if no clipping is applied.
     */
    Region getClipRegion();
}
