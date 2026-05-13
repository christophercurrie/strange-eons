package ca.cgjennings.apps.arkham.observable;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.strangeeons.plugins.observable.Observable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundObservableTest {

    @Test
    void getReturnsReaderResult() {
        Source src = new Source();
        src.name = "alpha";
        Observable<String> obs = src.observableName();
        assertEquals("alpha", obs.get());
    }

    @Test
    void subscribeReceivesPropertyChangeEvents() {
        Source src = new Source();
        Observable<String> obs = src.observableName();
        List<String> seen = new ArrayList<>();

        AutoCloseable token = obs.subscribe(seen::add);
        src.setName("beta");
        src.setName("gamma");

        assertEquals(List.of("beta", "gamma"), seen);

        try { token.close(); } catch (Exception ignored) {}
    }

    @Test
    void closeUnregistersListener() throws Exception {
        Source src = new Source();
        Observable<String> obs = src.observableName();
        List<String> seen = new ArrayList<>();

        AutoCloseable token = obs.subscribe(seen::add);
        src.setName("beta");
        token.close();
        src.setName("gamma");

        assertEquals(List.of("beta"), seen);
    }

    @Test
    void onlyFiresForBoundProperty() {
        Source src = new Source();
        Observable<String> name = src.observableName();
        List<String> seen = new ArrayList<>();
        name.subscribe(seen::add);

        // Firing an unrelated property should not notify the name observer.
        src.setOther("changed");
        assertTrue(seen.isEmpty());

        src.setName("beta");
        assertEquals(List.of("beta"), seen);
    }

    /** Minimal bean with PropertyChangeSupport. */
    private static final class Source {
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        private String name = "";
        private String other = "";

        String getName() { return name; }

        void setName(String value) {
            String old = name;
            name = value;
            pcs.firePropertyChange("name", old, value);
        }

        void setOther(String value) {
            String old = other;
            other = value;
            pcs.firePropertyChange("other", old, value);
        }

        void addPropertyChangeListener(String prop, PropertyChangeListener l) {
            pcs.addPropertyChangeListener(prop, l);
        }
        void removePropertyChangeListener(String prop, PropertyChangeListener l) {
            pcs.removePropertyChangeListener(prop, l);
        }

        Observable<String> observableName() {
            return new BoundObservable<>(
                    this::getName,
                    this::addPropertyChangeListener,
                    this::removePropertyChangeListener,
                    "name");
        }
    }
}
