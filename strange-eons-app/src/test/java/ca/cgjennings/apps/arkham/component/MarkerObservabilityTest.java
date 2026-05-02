package ca.cgjennings.apps.arkham.component;

import gamedata.Silhouette;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link Marker}'s setters fire property-change events for
 * {@code frontText}, {@code backText}, {@code stencilKey}, and
 * {@code bleedMargin}.
 *
 * <p>Marker's real constructor walks the {@link Silhouette} registry and loads
 * default portrait images via {@code ResourceKit}, which requires the full
 * application bootstrap. Tests here use Mockito's Objenesis-backed
 * instantiation to skip the constructor and seed the minimum field state by
 * reflection. That's enough to exercise the targeted setter logic without
 * pulling the resource loading machinery into the unit-test classpath.
 */
class MarkerObservabilityTest {

    @Test
    void setFrontTextFiresFrontTextEvent() throws Exception {
        Marker m = newSeededMarker();
        Recorder r = new Recorder();
        m.addPropertyChangeListener(r);

        m.setFrontText("hello");

        List<PropertyChangeEvent> events = r.eventsFor("frontText");
        assertEquals(1, events.size());
        assertEquals("", events.get(0).getOldValue());
        assertEquals("hello", events.get(0).getNewValue());
        assertSame(m, events.get(0).getSource());
    }

    @Test
    void setFrontTextNoEventWhenUnchanged() throws Exception {
        Marker m = newSeededMarker();
        m.setFrontText("same");
        Recorder r = new Recorder();
        m.addPropertyChangeListener(r);

        m.setFrontText("same");

        assertTrue(r.events.isEmpty());
    }

    @Test
    void setBackTextFiresBackTextEvent() throws Exception {
        Marker m = newSeededMarker();
        Recorder r = new Recorder();
        m.addPropertyChangeListener(r);

        m.setBackText("verso");

        List<PropertyChangeEvent> events = r.eventsFor("backText");
        assertEquals(1, events.size());
        assertEquals("verso", events.get(0).getNewValue());
    }

    @Test
    void setSilhouetteFiresStencilKeyAndBleedMarginEvents() throws Exception {
        Marker m = newSeededMarker();

        Silhouette sil = mock(Silhouette.class);
        when(sil.getKey()).thenReturn("@new-key");
        when(sil.getStencil()).thenReturn(null);
        when(sil.getBleedMargin()).thenReturn(7.5);

        Recorder r = new Recorder();
        m.addPropertyChangeListener(r);

        m.setSilhouette(sil);

        List<PropertyChangeEvent> stencil = r.eventsFor("stencilKey");
        assertEquals(1, stencil.size());
        assertEquals("@seed-key", stencil.get(0).getOldValue());
        assertEquals("@new-key", stencil.get(0).getNewValue());

        List<PropertyChangeEvent> bleed = r.eventsFor("bleedMargin");
        assertEquals(1, bleed.size());
        assertEquals(0.0, bleed.get(0).getOldValue());
        assertEquals(7.5, bleed.get(0).getNewValue());
    }

    @Test
    void setSilhouetteNoEventWhenStencilKeyUnchanged() throws Exception {
        Marker m = newSeededMarker();

        Silhouette sil = mock(Silhouette.class);
        when(sil.getKey()).thenReturn("@seed-key");

        Recorder r = new Recorder();
        m.addPropertyChangeListener(r);

        m.setSilhouette(sil);

        assertTrue(r.events.isEmpty(),
                "setSilhouette must short-circuit when the key is unchanged");
    }

    @Test
    void setNameFromBaseStillFiresOnMarker() throws Exception {
        Marker m = newSeededMarker();
        Recorder r = new Recorder();
        m.addPropertyChangeListener(r);

        m.setName("token");

        assertEquals(1, r.eventsFor("name").size());
        assertEquals("token", r.eventsFor("name").get(0).getNewValue());
    }

    /**
     * Builds a Marker that bypasses the constructor (so no Silhouette registry
     * lookup or default-portrait load is performed) and seeds the fields the
     * targeted setters read.
     */
    private static Marker newSeededMarker() throws Exception {
        Marker m = mock(Marker.class, CALLS_REAL_METHODS);
        setField(AbstractGameComponent.class, m, "name", "");
        setField(AbstractGameComponent.class, m, "comments", "");
        setField(Marker.class, m, "frontText", "");
        setField(Marker.class, m, "backText", "");
        setField(Marker.class, m, "stencilKey", "@seed-key");
        setField(Marker.class, m, "bleedMargin", 0.0);
        return m;
    }

    private static void setField(Class<?> owner, Object instance, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        f.set(instance, value);
    }

    private static final class Recorder implements PropertyChangeListener {
        final List<PropertyChangeEvent> events = new ArrayList<>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            events.add(evt);
        }

        List<PropertyChangeEvent> eventsFor(String name) {
            List<PropertyChangeEvent> out = new ArrayList<>();
            for (PropertyChangeEvent e : events) {
                if (name.equals(e.getPropertyName())) {
                    out.add(e);
                }
            }
            return out;
        }
    }
}
