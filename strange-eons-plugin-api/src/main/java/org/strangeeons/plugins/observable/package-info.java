/**
 * Observable-value primitives for plug-ins.
 *
 * <p>{@link Observable}{@code <T>} pairs a current value with a
 * change-subscription mechanism: callers read the current value via
 * {@link Observable#get()} and register a listener via
 * {@link Observable#subscribe(java.util.function.Consumer)} that
 * receives notifications on subsequent changes. The returned
 * {@link AutoCloseable} unregisters the listener.
 *
 * <p>{@link MutableObservable}{@code <T>} adds {@code set(T)} for
 * code that publishes observable state.
 *
 * @since 3.6
 */
package org.strangeeons.plugins.observable;
