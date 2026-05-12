package org.strangeeons.plugins.graphics;

/**
 * Describes a non-rectangular layout region that
 * {@link MarkupRenderer} wraps text within. The shape can be
 * arbitrary — a circle for round tokens, an irregular polygon for a
 * card with a decorative edge, etc. — and is supplied by the
 * framework's settings or layout helpers rather than constructed by
 * plug-ins directly.
 *
 * <p>{@code PageShape} is opaque at the SPI level; concrete shape
 * geometry is the framework's responsibility.
 *
 * @since 3.6
 */
public interface PageShape {
}
