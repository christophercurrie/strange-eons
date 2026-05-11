package org.strangeeons.plugins.graphics;

/**
 * An opaque shape passed to {@link Graphics#fill(Shape, Paint)} or
 * {@link Graphics#draw(Shape, Stroke)}. The framework constructs
 * shapes from its own geometry primitives; plug-ins receive them
 * from settings-driven layout helpers and pass them through to draw
 * operations.
 *
 * <p>Currently a marker interface with no plug-in-side construction
 * primitives; framework-side helpers produce shape instances from
 * settings-driven layout data.
 *
 * @since 3.6
 */
public interface Shape {
}
