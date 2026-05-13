package ca.cgjennings.apps.arkham.observable;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.strangeeons.plugins.observable.MutableObservable;

/**
 * A {@link MutableObservable} backed by an {@link AtomicReference}
 * plus a copy-on-write subscriber list. Suitable for any observable
 * value that does not already have a backing subscription mechanism
 * (such as {@link java.beans.PropertyChangeSupport}).
 *
 * <p>Listeners fire synchronously on the thread that calls
 * {@link #set(Object)}. The class is thread-safe for concurrent
 * {@link #set} and {@link #subscribe} calls but does not impose any
 * ordering between listeners observing concurrent updates from
 * different threads — a listener may see a newer value before
 * receiving the event for an older one if a faster thread races
 * past it.
 *
 * @param <T> the observed value type
 */
public final class AtomicObservable<T> implements MutableObservable<T> {

    private final AtomicReference<T> ref;
    private final CopyOnWriteArrayList<Consumer<? super T>> listeners
            = new CopyOnWriteArrayList<>();

    public AtomicObservable(T initial) {
        this.ref = new AtomicReference<>(initial);
    }

    @Override
    public T get() {
        return ref.get();
    }

    @Override
    public void set(T value) {
        T previous = ref.getAndSet(value);
        if (!Objects.equals(previous, value)) {
            for (Consumer<? super T> listener : listeners) {
                listener.accept(value);
            }
        }
    }

    @Override
    public AutoCloseable subscribe(Consumer<? super T> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
