package org.strangeeons.plugins.graphics;

/**
 * A composite text style: font family, size, weight, posture,
 * color, decorations, and any other typographic attributes
 * applicable to a run of text. Plug-ins typically receive styles
 * from settings via the framework's settings accessors and pass
 * them through to {@link MarkupRenderer} without inspecting the
 * contents.
 *
 * <p>{@code TextStyle} is opaque at the SPI level: the framework
 * decides which attribute model backs the style (today AWT
 * {@code TextAttribute} maps; potentially a different model in a
 * future Compose backend). Plug-ins do not construct styles
 * directly through the SPI; the framework constructs them in
 * response to settings lookups.
 *
 * @since 3.6
 */
public interface TextStyle {
}
