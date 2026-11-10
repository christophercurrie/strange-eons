package org.strangeeons.core.editor;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.observable.MutableObservable;

/**
 * Keeps a setting and an observable value in step, marking faces for
 * repainting when the value changes the setting.
 */
public final class SettingBinding {

    private final String key;
    private final MutableObservable<String> value;
    private final Supplier<? extends SettingsReadWrite> settings;
    private final IntSupplier faceCount;
    private final IntConsumer markChanged;
    private final int[] faces;
    private boolean populating;

    /**
     * Binds a value to a setting, then {@linkplain #populate populates} it.
     *
     * @param key the setting key
     * @param value the value to keep in step with the setting
     * @param settings supplies the settings the key is stored in
     * @param faceCount supplies the number of faces, or a negative number if
     * it is not yet known
     * @param markChanged marks a face as needing to be repainted
     * @param faces the faces that show the setting; if empty, all faces
     * @throws IllegalArgumentException if a face does not exist
     */
    public SettingBinding(String key, MutableObservable<String> value, Supplier<? extends SettingsReadWrite> settings,
            IntSupplier faceCount, IntConsumer markChanged, int... faces) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Objects.requireNonNull(value, "value");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.faceCount = Objects.requireNonNull(faceCount, "faceCount");
        this.markChanged = Objects.requireNonNull(markChanged, "markChanged");
        this.faces = faces.clone();
        int count = faceCount.getAsInt();
        for (int face : faces) {
            if (face < 0 || (count >= 0 && face >= count)) {
                throw new IllegalArgumentException("face does not exist: " + face);
            }
        }
        populate();
        value.subscribe(this::store);
    }

    /**
     * Updates the value from the setting. If the setting has no value, it is
     * set from the value instead.
     */
    public void populate() {
        SettingsReadWrite s = settings.get();
        String stored = s.get(key);
        if (stored == null) {
            String initial = value.get();
            if (initial != null) {
                s.set(key, initial);
            }
            return;
        }
        populating = true;
        try {
            value.set(stored);
        } finally {
            populating = false;
        }
    }

    private void store(String newValue) {
        if (populating || newValue == null) {
            return;
        }
        SettingsReadWrite s = settings.get();
        if (newValue.equals(s.get(key))) {
            return;
        }
        s.set(key, newValue);
        markFaces();
    }

    private void markFaces() {
        if (faces.length > 0) {
            for (int face : faces) {
                markChanged.accept(face);
            }
            return;
        }
        int count = faceCount.getAsInt();
        if (count < 0) {
            count = 1;
        }
        for (int face = 0; face < count; ++face) {
            markChanged.accept(face);
        }
    }
}
