package org.strangeeons.plugins.conventional;

import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.graphics.ImageHandle;
import org.strangeeons.plugins.graphics.Region;

/**
 * Per-face context passed to a {@link ComponentHandler}'s painter
 * and paint methods. Carries the dispatch information ({@link
 * #getFaceIndex()}, {@link #getTemplateKey()}, {@link
 * #getTemplateResolution()}) plus settings-namespaced lookups for
 * the regions and images this face uses.
 *
 * <p>{@code Face} is a data carrier — it identifies which face the
 * handler is painting and surfaces its associated data, but does not
 * itself perform rendering. The handler does all drawing through the
 * {@link org.strangeeons.plugins.graphics.Graphics} passed to its
 * paint method.
 *
 * <p>The framework guarantees that {@code Face} instances are stable
 * across paint cycles for the lifetime of the component, so plug-ins
 * may cache per-face state keyed by the instance or by
 * {@link #getFaceIndex()}.
 *
 * @since 3.6
 */
public interface Face {

    /**
     * Returns the zero-based face index, in the range
     * {@code [0, handler.getFaceCount())}.
     */
    int getFaceIndex();

    /**
     * Returns the settings key prefix for this face, as declared by
     * the handler's {@link ComponentHandler#getTemplateKey(int)};
     * may be {@code null} if the handler opted out of framework-
     * provided template painting.
     */
    String getTemplateKey();

    /**
     * Returns the component-wide settings scope. Reads fall back
     * through parent scopes; writes affect only this scope and
     * persist with the component. Equivalent across faces of the
     * same component.
     */
    SettingsReadWrite getSettings();

    /** Returns the template resolution in pixels per inch. */
    int getTemplateResolution();

    /**
     * Resolves a region from this face's settings namespace. The
     * lookup uses the face's {@link #getTemplateKey()} as a prefix;
     * for example, {@code face.region("title")} on a face whose
     * template key is {@code "lore-front"} resolves the
     * {@code lore-front-title-region} setting.
     */
    Region region(String key);

    /**
     * Resolves an image from this face's settings namespace. The
     * lookup uses the face's {@link #getTemplateKey()} as a prefix;
     * for example, {@code face.image("background")} on a face whose
     * template key is {@code "lore-front"} resolves the
     * {@code lore-front-background} setting.
     */
    ImageHandle image(String key);

    /**
     * Returns the template image for this face — the underlying card
     * art that the handler typically draws into the face buffer before
     * adding portrait, text, and decoration on top.
     */
    ImageHandle getTemplateImage();
}
