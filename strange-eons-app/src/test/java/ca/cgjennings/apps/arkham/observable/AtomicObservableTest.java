package ca.cgjennings.apps.arkham.observable;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.strangeeons.plugins.observable.MutableObservable;
import org.strangeeons.plugins.observable.Observable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicObservableTest {

    @Test
    void getReturnsInitialValue() {
        AtomicObservable<String> obs = new AtomicObservable<>("initial");
        assertEquals("initial", obs.get());
    }

    @Test
    void setUpdatesCurrentValue() {
        AtomicObservable<String> obs = new AtomicObservable<>("a");
        obs.set("b");
        assertEquals("b", obs.get());
    }

    @Test
    void subscribeReceivesSubsequentChanges() {
        AtomicObservable<Integer> obs = new AtomicObservable<>(0);
        List<Integer> seen = new ArrayList<>();

        AutoCloseable token = obs.subscribe(seen::add);
        obs.set(1);
        obs.set(2);
        obs.set(3);

        assertEquals(List.of(1, 2, 3), seen);

        try { token.close(); } catch (Exception ignored) {}
    }

    @Test
    void subscribeDoesNotFireForInitialValue() {
        AtomicObservable<String> obs = new AtomicObservable<>("initial");
        List<String> seen = new ArrayList<>();
        obs.subscribe(seen::add);
        // No set call — listener should not have fired.
        assertTrue(seen.isEmpty());
    }

    @Test
    void closeStopsNotifications() throws Exception {
        AtomicObservable<String> obs = new AtomicObservable<>("a");
        List<String> seen = new ArrayList<>();
        AutoCloseable token = obs.subscribe(seen::add);
        obs.set("b");
        token.close();
        obs.set("c");

        assertEquals(List.of("b"), seen);
    }

    @Test
    void repeatedCloseIsNoOp() throws Exception {
        AtomicObservable<String> obs = new AtomicObservable<>("a");
        AutoCloseable token = obs.subscribe(v -> {});
        token.close();
        token.close();   // must not throw
    }

    @Test
    void settingSameValueDoesNotFire() {
        AtomicObservable<String> obs = new AtomicObservable<>("a");
        List<String> seen = new ArrayList<>();
        obs.subscribe(seen::add);
        obs.set("a");      // same value
        obs.set("b");      // changed
        obs.set("b");      // same again
        assertEquals(List.of("b"), seen);
    }

    @Test
    void settingNullClearsValue() {
        AtomicObservable<String> obs = new AtomicObservable<>("a");
        List<String> seen = new ArrayList<>();
        obs.subscribe(seen::add);
        obs.set(null);
        assertEquals(1, seen.size());
        assertEquals(null, seen.get(0));
        assertEquals(null, obs.get());
    }

    @Test
    void multipleSubscribersEachReceiveEvents() {
        AtomicObservable<String> obs = new AtomicObservable<>("a");
        List<String> seenA = new ArrayList<>();
        List<String> seenB = new ArrayList<>();
        obs.subscribe(seenA::add);
        obs.subscribe(seenB::add);
        obs.set("b");
        assertEquals(List.of("b"), seenA);
        assertEquals(List.of("b"), seenB);
    }

    @Test
    void exposesAsObservable() {
        MutableObservable<String> mutable = new AtomicObservable<>("a");
        Observable<String> readOnly = mutable;
        assertEquals("a", readOnly.get());
    }
}
