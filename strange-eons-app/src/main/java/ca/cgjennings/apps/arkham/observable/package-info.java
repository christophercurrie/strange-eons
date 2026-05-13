/**
 * Framework-side implementations of the
 * {@link org.strangeeons.plugins.observable} SPI.
 *
 * <p>{@link AtomicObservable} is a self-contained
 * {@link org.strangeeons.plugins.observable.MutableObservable}
 * suitable for any value that does not have an existing subscription
 * mechanism. {@link BoundObservable} adapts a property already
 * exposed through {@link java.beans.PropertyChangeSupport} (as on
 * {@link ca.cgjennings.apps.arkham.component.AbstractGameComponent})
 * into a read-only
 * {@link org.strangeeons.plugins.observable.Observable}.
 */
package ca.cgjennings.apps.arkham.observable;
