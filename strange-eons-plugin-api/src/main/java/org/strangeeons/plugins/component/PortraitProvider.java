package org.strangeeons.plugins.component;

/**
 * Implemented by components or handlers that expose one or more
 * {@link Portrait portraits}. The provider abstraction lets the
 * framework discover portraits uniformly across component frameworks.
 *
 * <p>This is the no-arg shape; component frameworks that wish to
 * accept a component reference per call (so a handler implementation
 * can be stateless) typically define their own portrait-access
 * methods on the framework's handler interface instead.
 *
 * @since 3.6
 */
public interface PortraitProvider {

    /** Returns the number of portraits exposed. */
    int getPortraitCount();

    /**
     * Returns the portrait at the given index, where {@code 0 <=
     * index < getPortraitCount()}.
     *
     * @throws IndexOutOfBoundsException if {@code index} is out of
     *     range
     */
    Portrait getPortrait(int index);
}
