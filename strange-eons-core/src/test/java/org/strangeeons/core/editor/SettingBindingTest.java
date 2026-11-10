package org.strangeeons.core.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.strangeeons.core.observable.AtomicObservable;
import org.strangeeons.plugins.SettingsReadWrite;

class SettingBindingTest {

    private final Map<String, String> store = new HashMap<>();
    private final SettingsReadWrite settings = new MapSettings(store);
    private final List<Integer> marked = new ArrayList<>();

    @Test
    void writesTheInitialValueWhenTheSettingIsMissing() {
        AtomicObservable<String> value = new AtomicObservable<>("default");
        new SettingBinding("k", value, () -> settings, () -> 2, marked::add);
        assertThat(store).containsEntry("k", "default");
        assertThat(marked).isEmpty();
    }

    @Test
    void leavesMissingSettingsAloneWithoutAnInitialValue() {
        new SettingBinding("k", new AtomicObservable<>(null), () -> settings, () -> 2, marked::add);
        assertThat(store).doesNotContainKey("k");
    }

    @Test
    void populatesFromTheSettingWithoutMarkingFaces() {
        store.put("k", "stored");
        AtomicObservable<String> value = new AtomicObservable<>("default");
        SettingBinding binding = new SettingBinding("k", value, () -> settings, () -> 2, marked::add);
        assertThat(value.get()).isEqualTo("stored");
        store.put("k", "again");
        binding.populate();
        assertThat(value.get()).isEqualTo("again");
        assertThat(marked).isEmpty();
    }

    @Test
    void storesChangesAndMarksTheBoundFaces() {
        AtomicObservable<String> value = new AtomicObservable<>("a");
        new SettingBinding("k", value, () -> settings, () -> 3, marked::add, 1, 2);
        value.set("b");
        assertThat(store).containsEntry("k", "b");
        assertThat(marked).containsExactly(1, 2);
        value.set(null);
        assertThat(store).containsEntry("k", "b");
        assertThat(marked).containsExactly(1, 2);
    }

    @Test
    void marksEveryFaceWhenNoneAreGiven() {
        AtomicObservable<String> value = new AtomicObservable<>("a");
        new SettingBinding("k", value, () -> settings, () -> 3, marked::add);
        value.set("b");
        assertThat(marked).containsExactly(0, 1, 2);
    }

    @Test
    void marksOneFaceWhenTheCountIsUnknown() {
        AtomicObservable<String> value = new AtomicObservable<>("a");
        new SettingBinding("k", value, () -> settings, () -> -1, marked::add, 5);
        value.set("b");
        assertThat(marked).containsExactly(5);
        AtomicObservable<String> other = new AtomicObservable<>("x");
        new SettingBinding("j", other, () -> settings, () -> -1, marked::add);
        marked.clear();
        other.set("y");
        assertThat(marked).containsExactly(0);
    }

    @Test
    void ignoresValuesThatMatchTheSetting() {
        store.put("k", "same");
        AtomicObservable<String> value = new AtomicObservable<>("x");
        new SettingBinding("k", value, () -> settings, () -> 1, marked::add);
        store.put("k", "external");
        value.set("external");
        assertThat(marked).isEmpty();
    }

    @Test
    void rejectsMissingFaces() {
        AtomicObservable<String> value = new AtomicObservable<>("a");
        assertThatThrownBy(() -> new SettingBinding("k", value, () -> settings, () -> 2, marked::add, 2))
                .hasMessageContaining("face does not exist: 2");
        assertThatThrownBy(() -> new SettingBinding("k", value, () -> settings, () -> 2, marked::add, -1))
                .hasMessageContaining("face does not exist: -1");
        assertThatThrownBy(() -> new SettingBinding(null, value, () -> settings, () -> 2, marked::add))
                .isInstanceOf(NullPointerException.class);
    }

    /** Minimal map-backed settings; only get and set are used by bindings. */
    private static final class MapSettings implements SettingsReadWrite {
        private final Map<String, String> map;

        MapSettings(Map<String, String> map) {
            this.map = map;
        }

        @Override public String get(String key) { return map.get(key); }
        @Override public void set(String key, String value) { map.put(key, value); }
        @Override public String get(String key, String d) { throw new UnsupportedOperationException(); }
        @Override public String getText(String key) { throw new UnsupportedOperationException(); }
        @Override public boolean getYesNo(String key) { throw new UnsupportedOperationException(); }
        @Override public int getInt(String key) { throw new UnsupportedOperationException(); }
        @Override public float getFloat(String key) { throw new UnsupportedOperationException(); }
        @Override public double getDouble(String key) { throw new UnsupportedOperationException(); }
        @Override public java.util.Set<String> getKeySet() { throw new UnsupportedOperationException(); }
        @Override public java.util.Set<String> getVisibleKeySet() { throw new UnsupportedOperationException(); }
        @Override public org.strangeeons.plugins.SettingsReader getParent() { throw new UnsupportedOperationException(); }
        @Override public org.strangeeons.plugins.graphics.TextStyle readTextStyle(String key, org.strangeeons.plugins.graphics.TextStyle b) { throw new UnsupportedOperationException(); }
        @Override public org.strangeeons.plugins.graphics.PageShape readPageShape(String key) { throw new UnsupportedOperationException(); }
        @Override public int readAlignment(String key) { throw new UnsupportedOperationException(); }
        @Override public org.strangeeons.plugins.graphics.ImageHandle readImage(String key) { throw new UnsupportedOperationException(); }
        @Override public void setText(String key, String value) { throw new UnsupportedOperationException(); }
        @Override public void setYesNo(String key, boolean value) { throw new UnsupportedOperationException(); }
        @Override public void setInt(String key, int value) { throw new UnsupportedOperationException(); }
        @Override public void setFloat(String key, float value) { throw new UnsupportedOperationException(); }
        @Override public void setDouble(String key, double value) { throw new UnsupportedOperationException(); }
        @Override public void reset(String key) { throw new UnsupportedOperationException(); }
        @Override public void addSettingsFrom(String resource) { throw new UnsupportedOperationException(); }
    }
}
