package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.util.Objects;
import org.strangeeons.plugins.observable.MutableObservable;
import resources.Settings;

/**
 * Keeps a setting and an observable value in step, marking faces for
 * repainting when the value changes the setting.
 */
final class SettingBinding {

    private final String key;
    private final MutableObservable<String> value;
    private final DIY diy;
    private final int[] faces;
    private boolean populating;

    SettingBinding(String key, MutableObservable<String> value, DIY diy, int[] faces) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Objects.requireNonNull(value, "value");
        this.diy = diy;
        this.faces = faces.clone();
        checkFaces(diy, faces);
        populate();
        value.subscribe(this::store);
    }

    private static void checkFaces(DIY diy, int[] faces) {
        Sheet<?>[] sheets = diy.getSheets();
        for (int face : faces) {
            if (face < 0 || (sheets != null && face >= sheets.length)) {
                throw new IllegalArgumentException("face does not exist: " + face);
            }
        }
    }

    /** Copies the setting to the value, or the value to an unset setting. */
    void populate() {
        Settings settings = diy.getSettings();
        String stored = settings.get(key);
        if (stored == null) {
            String initial = value.get();
            if (initial != null) {
                settings.set(key, initial);
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
        Settings settings = diy.getSettings();
        if (newValue.equals(settings.get(key))) {
            return;
        }
        settings.set(key, newValue);
        markFaces();
    }

    private void markFaces() {
        if (faces.length > 0) {
            for (int face : faces) {
                diy.markChanged(face);
            }
            return;
        }
        Sheet<?>[] sheets = diy.getSheets();
        int count = sheets == null ? 1 : sheets.length;
        for (int face = 0; face < count; ++face) {
            diy.markChanged(face);
        }
    }
}
