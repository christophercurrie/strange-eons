package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.ui.theme.ThemedIcon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AbstractInstalledPluginTest {

    /**
     * Regression: opening Plugin Manager threw NullPointerException after
     * the user disabled a tool plug-in. collectPluginInfo() probes the
     * plug-in to cache its metadata, then stops it again because the
     * plug-in is reloadable, leaving the plugin field null. getIcon() must
     * fall through to the bundle/default icon instead of dereferencing it.
     */
    @Test
    void getIconDoesNotNpeWhenPluginInstanceIsStopped() throws Exception {
        InstalledPlugin ip = new InstalledPlugin(null, NoopPlugin.class.getName());
        ip.getName();

        ThemedIcon icon = assertDoesNotThrow(ip::getIcon);
        assertNotNull(icon);
    }

    public static final class NoopPlugin implements Plugin {

        @Override
        public boolean initializePlugin(PluginContext context) {
            return true;
        }

        @Override
        public void unloadPlugin() {
        }

        @Override
        public String getPluginName() {
            return "Test";
        }

        @Override
        public String getPluginDescription() {
            return "Test";
        }

        @Override
        public float getPluginVersion() {
            return 1.0f;
        }

        @Override
        public int getPluginType() {
            return ACTIVATED;
        }

        @Override
        public void showPlugin(PluginContext context, boolean show) {
        }

        @Override
        public boolean isPluginShowing() {
            return false;
        }

        @Override
        public boolean isPluginUsable() {
            return true;
        }

        @Override
        public ThemedIcon getPluginIcon() {
            return null;
        }

        @Override
        public String getDefaultAcceleratorKey() {
            return null;
        }
    }
}
