package ca.cgjennings.apps.arkham.dialog.prefs;

import java.awt.Component;
import java.awt.Container;
import java.util.Properties;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import resources.Settings;
import static org.assertj.core.api.Assertions.assertThat;

class CatDesignSupportTest {
    @Test void defaultsToTopTabs() throws Exception {
        Properties defaults = new Properties();
        try (var stream = getClass().getResourceAsStream("/resources/default.settings")) {
            defaults.load(stream);
        }

        assertThat(defaults.getProperty("component-navigation")).isEqualTo("tabs");
    }

    @Test void navigationPreferenceRoundTrips() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Settings settings = Settings.getUser();
            String old = settings.get("component-navigation");
            try {
                CatDesignSupport category = new CatDesignSupport();
                settings.set("component-navigation", "side");
                category.loadSettings();
                JComboBox<?> choice = findChoice(category.getPanel());

                assertThat(choice).isNotNull();
                assertThat(choice.getSelectedIndex()).isEqualTo(1);

                choice.setSelectedIndex(0);
                category.storeSettings();

                assertThat(settings.get("component-navigation")).isEqualTo("tabs");

                choice.setSelectedIndex(1);
                category.storeSettings();

                assertThat(settings.get("component-navigation")).isEqualTo("side");
            } finally {
                if (old == null) settings.reset("component-navigation");
                else settings.set("component-navigation", old);
            }
        });
    }

    private JComboBox<?> findChoice(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JComboBox<?>) return (JComboBox<?>) child;
            if (child instanceof Container) {
                JComboBox<?> result = findChoice((Container) child);
                if (result != null) return result;
            }
        }
        return null;
    }
}
