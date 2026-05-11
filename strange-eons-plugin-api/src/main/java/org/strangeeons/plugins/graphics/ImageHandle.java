package org.strangeeons.plugins.graphics;

/**
 * Opaque reference to a bitmap image. The handle is identity-shaped:
 * plug-ins pass it through to draw operations
 * ({@link Graphics#drawImage(ImageHandle, double, double)}, …) and
 * compare instances by reference equality. The framework owns the
 * pixel data and may resolve, decode, or cache it lazily.
 *
 * <p>Plug-ins do not construct image handles directly through the
 * SPI; they receive them from
 * {@link org.strangeeons.plugins.component.Portrait#getImageHandle()},
 * settings accessors, or the framework's resource pipeline.
 * Toolkit-specific operation libraries (when available) provide
 * read-only or transforming access to the underlying pixels.
 *
 * <p>Handles may serialize as references (URL, file path, resource
 * key) when one exists, falling back to an inline pixel blob
 * otherwise. The serialized form is the framework's concern; plug-ins
 * treat handles as opaque values.
 *
 * @since 3.6
 */
public interface ImageHandle {

    /** Returns the image width in pixels. */
    int getWidth();

    /** Returns the image height in pixels. */
    int getHeight();

    /**
     * Returns an identifier for the source that produced this image
     * — typically a resource key, file path, or URL string. May
     * return {@code null} for derived or synthesised handles that do
     * not originate from a named source.
     */
    String getSource();
}
