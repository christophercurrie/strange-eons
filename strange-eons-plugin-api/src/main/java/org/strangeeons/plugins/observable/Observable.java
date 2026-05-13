package org.strangeeons.plugins.observable;

import java.util.function.Consumer;

/**
 * A value paired with a change-subscription mechanism. Callers read
 * the current value via {@link #get()} and register a listener via
 * {@link #subscribe(Consumer)} to be notified when the value
 * changes. The returned {@link AutoCloseable} unregisters the
 * listener; callers must close it to avoid leaks.
 *
 * <p>The subscription contract:
 *
 * <ul>
 *   <li>Listeners are <b>not</b> notified of the current value at
 *       subscribe time — callers should consult {@link #get()}
 *       directly if they need an initial value.</li>
 *   <li>Listeners are notified on <b>every change</b> to the value
 *       after subscription, in the order changes occur on the
 *       producing thread.</li>
 *   <li>Implementations may choose to skip notifications when the
 *       value is set to a {@link java.util.Objects#equals
 *       equal} value; consumers must not rely on every {@code set}
 *       producing an event.</li>
 *   <li>Notifications fire synchronously on the thread that mutated
 *       the value, unless the implementation documents otherwise.</li>
 * </ul>
 *
 * @param <T> the observed value type
 * @since 3.6
 */
public interface Observable<T> {

    /** Returns the current value. */
    T get();

    /**
     * Registers a listener that fires on each subsequent change to
     * the value. The returned {@link AutoCloseable} unregisters the
     * listener when closed. Implementations must accept repeated
     * close calls without raising.
     */
    AutoCloseable subscribe(Consumer<? super T> listener);
}
