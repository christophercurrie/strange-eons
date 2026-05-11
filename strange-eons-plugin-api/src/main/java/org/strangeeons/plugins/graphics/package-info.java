/**
 * Toolkit-neutral graphics SPI for Strange Eons plug-ins.
 *
 * <p>Types in this package describe drawing surfaces, value types
 * (color, font, geometry, stroke, transform), and image handles
 * without committing to a specific UI toolkit. Plug-in code calls
 * these interfaces; the framework supplies implementations backed by
 * whichever rendering pipeline is active.
 *
 * <h2>Value-type vocabulary</h2>
 *
 * Simple value types ({@link Region}, colors, points, dimensions,
 * fonts) cross the SPI as strings matching the format used in
 * {@code .settings} files. Each typed interface in this package
 * exposes static factory methods that parse the canonical format and
 * a {@code toSetting()} method that produces it. The framework owns
 * parse and format; plug-ins go through the factory methods.
 *
 * <p>Format evolution is additive: extensions are tolerated by older
 * parsers. The string format is not authoritative; the factory and
 * formatter helpers in each type are.
 *
 * <h2>Graphics surface</h2>
 *
 * {@link Graphics} is the painter wrapper. Paint, stroke, and font
 * are per-call parameters on shape primitives; transforms, clips,
 * alpha, and blend modes apply via lambda-scoped {@code withX} blocks
 * that automatically restore state on exit.
 *
 * <h2>Images</h2>
 *
 * Bitmaps cross the SPI as opaque {@link ImageHandle} instances. The
 * handle is identity-shaped; toolkit-specific operation libraries
 * provide pixel access. Image handles may serialize as references
 * (URL, file path, resource key) when one exists, falling back to a
 * pixel blob otherwise.
 *
 * @since 3.6
 */
package org.strangeeons.plugins.graphics;
