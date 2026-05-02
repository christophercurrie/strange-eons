package ca.cgjennings.apps.arkham.component;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the PropertyChangeSupport plumbing added to {@code AbstractGameComponent}.
 * The base class is what every concrete model inherits, so its observability
 * contract is what matters: listener add/remove across both overloads, fire helpers
 * in both flavors, the lazy-init guarantee, the post-mark fire ordering, and the
 * no-listener allocation path.
 */
class AbstractGameComponentObservabilityTest {

    @Test
    void addedListenerReceivesNameChange() {
        Stub c = new Stub();
        Recorder r = new Recorder();
        c.addPropertyChangeListener(r);

        c.setName("alpha");

        assertEquals(1, r.events.size());
        PropertyChangeEvent e = r.events.get(0);
        assertEquals("name", e.getPropertyName());
        assertEquals("", e.getOldValue());
        assertEquals("alpha", e.getNewValue());
        assertSame(c, e.getSource());
    }

    @Test
    void noEventWhenNameIsUnchanged() {
        Stub c = new Stub();
        c.setName("seed");
        Recorder r = new Recorder();
        c.addPropertyChangeListener(r);

        c.setName("seed");

        assertTrue(r.events.isEmpty());
    }

    @Test
    void commentChangeFiresEvent() {
        Stub c = new Stub();
        Recorder r = new Recorder();
        c.addPropertyChangeListener(r);

        c.setComment("note");

        assertEquals(1, r.events.size());
        assertEquals("comment", r.events.get(0).getPropertyName());
        assertEquals("note", r.events.get(0).getNewValue());
    }

    @Test
    void propertyScopedListenerOnlyFiresForMatchingName() {
        Stub c = new Stub();
        Recorder name = new Recorder();
        Recorder comment = new Recorder();
        c.addPropertyChangeListener("name", name);
        c.addPropertyChangeListener("comment", comment);

        c.setName("x");
        c.setComment("y");

        assertEquals(1, name.events.size());
        assertEquals("name", name.events.get(0).getPropertyName());
        assertEquals(1, comment.events.size());
        assertEquals("comment", comment.events.get(0).getPropertyName());
    }

    @Test
    void removedListenerStopsReceivingEvents() {
        Stub c = new Stub();
        Recorder r = new Recorder();
        c.addPropertyChangeListener(r);
        c.setName("first");
        c.removePropertyChangeListener(r);

        c.setName("second");

        assertEquals(1, r.events.size());
        assertEquals("first", r.events.get(0).getNewValue());
    }

    @Test
    void removePropertyScopedListenerStopsReceivingEvents() {
        Stub c = new Stub();
        Recorder r = new Recorder();
        c.addPropertyChangeListener("name", r);
        c.removePropertyChangeListener("name", r);

        c.setName("after-remove");

        assertTrue(r.events.isEmpty());
    }

    @Test
    void removeBeforeAnyAddIsSafe() {
        // Exercises the null-changeSupport branch in both removal overloads.
        // No listener has ever been attached, so nothing is allocated and
        // nothing throws.
        Stub c = new Stub();
        c.removePropertyChangeListener(new Recorder());
        c.removePropertyChangeListener("name", new Recorder());

        // And a subsequent setter still does nothing observable.
        c.setName("after-noop-removes");
        assertEquals("after-noop-removes", c.getName());
    }

    @Test
    void firePropertyChangeIsNoopWhenNoListenerAttached() {
        Stub c = new Stub();
        c.fireObject("k", "old", "new");
        c.fireDouble("d", 1.0, 2.0);
        // No exception, no listener — nothing to assert beyond reaching here.
    }

    @Test
    void doubleFireOverloadSuppressesEqualValues() {
        Stub c = new Stub();
        Recorder r = new Recorder();
        c.addPropertyChangeListener(r);

        c.fireDouble("d", 1.5, 1.5);

        assertTrue(r.events.isEmpty());
    }

    @Test
    void doubleFireOverloadEmitsOnDifference() {
        Stub c = new Stub();
        Recorder r = new Recorder();
        c.addPropertyChangeListener(r);

        c.fireDouble("d", 1.0, 2.5);

        assertEquals(1, r.events.size());
        assertEquals(1.0, r.events.get(0).getOldValue());
        assertEquals(2.5, r.events.get(0).getNewValue());
        assertEquals("d", r.events.get(0).getPropertyName());
    }

    @Test
    void listenerSeesPostMarkStateOfDirtyFlag() {
        // The contract is that listeners fire AFTER markChanged and
        // markUnsavedChanges. With no sheets attached, markChanged is a no-op
        // but markUnsavedChanges still runs from setName, so a re-entrant
        // listener must observe hasUnsavedChanges() == true.
        Stub c = new Stub();
        boolean[] unsavedAtFire = {false};
        c.addPropertyChangeListener("name", evt -> unsavedAtFire[0] = c.hasUnsavedChanges());

        c.setName("z");

        assertTrue(unsavedAtFire[0]);
    }

    @Test
    void nullNameStillThrowsAndDoesNotFire() {
        Stub c = new Stub();
        Recorder r = new Recorder();
        c.addPropertyChangeListener(r);

        assertThrows(NullPointerException.class, () -> c.setName(null));
        assertThrows(NullPointerException.class, () -> c.setComment(null));
        assertTrue(r.events.isEmpty());
    }

    @Test
    void changeSupportIsTransientAndLazyInitOnDeserializedInstance() throws Exception {
        Stub original = new Stub();
        original.addPropertyChangeListener(new Recorder()); // forces PCS allocation
        original.setName("seed");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        Stub copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (Stub) in.readObject();
        }

        // Original listener does not survive serialization; PCS field is transient.
        assertEquals("seed", copy.getName());

        // The copy is still a working observable: re-registering and mutating
        // produces an event, proving lazy-init recreated the support.
        Recorder r = new Recorder();
        copy.addPropertyChangeListener(r);
        copy.setName("post-deserialize");
        assertEquals(1, r.events.size());
        assertEquals("post-deserialize", r.events.get(0).getNewValue());
    }

    private static final class Recorder implements PropertyChangeListener {
        final List<PropertyChangeEvent> events = new ArrayList<>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            events.add(evt);
        }
    }

    /**
     * Minimal concrete subclass — just enough to satisfy the abstract contract
     * for the observability tests. Sheets are unused; the dirty-flag check
     * only looks at the {@code hasUndrawnChanges} side effect of
     * {@code markChanged}, which works without real sheets.
     */
    private static final class Stub extends AbstractGameComponent {
        private static final long serialVersionUID = 1L;

        @Override
        public Sheet[] createDefaultSheets() {
            return null;
        }

        @Override
        public String[] getSheetTitles() {
            return null;
        }

        @Override
        public AbstractGameComponentEditor<? extends GameComponent> createDefaultEditor() {
            return null;
        }

        // AbstractGameComponent's writeObject/readObject are intentionally
        // empty; subclasses must persist name/comments/privateSettings via the
        // protected write/read helpers, like Marker does.
        private void writeObject(ObjectOutputStream out) throws IOException {
            write(out);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            read(in);
        }

        void fireObject(String name, Object oldVal, Object newVal) {
            firePropertyChange(name, oldVal, newVal);
        }

        void fireDouble(String name, double oldVal, double newVal) {
            firePropertyChange(name, oldVal, newVal);
        }
    }
}
