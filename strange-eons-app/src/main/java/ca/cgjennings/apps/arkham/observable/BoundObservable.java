package ca.cgjennings.apps.arkham.observable;

import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.strangeeons.plugins.observable.Observable;

/**
 * Read-only {@link Observable} adapter that bridges a property
 * exposed through {@link java.beans.PropertyChangeSupport} into the
 * SPI observable contract. Current value is supplied by the reader
 * function; subscriptions register a {@link PropertyChangeListener}
 * on the source's add/remove hooks.
 *
 * <p>Typical construction wires the adapter onto a component:
 *
 * <pre>{@code
 * Observable<String> name = new BoundObservable<>(
 *         component::getName,
 *         component::addPropertyChangeListener,
 *         component::removePropertyChangeListener,
 *         "name");
 * }</pre>
 *
 * @param <T> the observed value type
 */
public final class BoundObservable<T> implements Observable<T> {

    private final Supplier<? extends T> reader;
    private final BiConsumer<String, PropertyChangeListener> addListener;
    private final BiConsumer<String, PropertyChangeListener> removeListener;
    private final String propertyName;

    public BoundObservable(
            Supplier<? extends T> reader,
            BiConsumer<String, PropertyChangeListener> addListener,
            BiConsumer<String, PropertyChangeListener> removeListener,
            String propertyName) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.addListener = Objects.requireNonNull(addListener, "addListener");
        this.removeListener = Objects.requireNonNull(removeListener, "removeListener");
        this.propertyName = Objects.requireNonNull(propertyName, "propertyName");
    }

    @Override
    public T get() {
        return reader.get();
    }

    @Override
    public AutoCloseable subscribe(Consumer<? super T> listener) {
        Objects.requireNonNull(listener, "listener");
        PropertyChangeListener pcl = evt -> {
            @SuppressWarnings("unchecked")
            T newValue = (T) evt.getNewValue();
            listener.accept(newValue);
        };
        addListener.accept(propertyName, pcl);
        return () -> removeListener.accept(propertyName, pcl);
    }
}
