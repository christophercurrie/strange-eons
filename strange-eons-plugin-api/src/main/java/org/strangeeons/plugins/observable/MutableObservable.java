package org.strangeeons.plugins.observable;

/**
 * An {@link Observable} whose value can be assigned. Code that
 * publishes observable state holds the {@code MutableObservable}
 * reference, while consumers receive the narrowed {@link Observable}
 * view.
 *
 * <p>Implementations must:
 *
 * <ul>
 *   <li>Update {@link #get()} to the supplied value before notifying
 *       subscribers.</li>
 *   <li>Skip notification when the new value is
 *       {@link java.util.Objects#equals equal} to the current value,
 *       to avoid spurious change events.</li>
 * </ul>
 *
 * @param <T> the observed value type
 * @since 3.6
 */
public interface MutableObservable<T> extends Observable<T> {

    /**
     * Sets the value. If the new value is equal to the current
     * value, the call has no observable effect; otherwise the value
     * is updated and subscribers are notified.
     */
    void set(T value);
}
