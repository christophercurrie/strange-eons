package ca.cgjennings.apps.arkham.plugins;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.strangeeons.plugins.AbstractPlugin;
import org.strangeeons.plugins.PluginContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the Tier 2 adapter path: when the plug-in loader
 * encounters a class that implements only
 * {@link org.strangeeons.plugins.Plugin}, it must instantiate it,
 * wrap it in a {@link LegacyPluginAdapter}, and present the adapter
 * to the rest of the application as a legacy {@link Plugin}.
 */
class NewStylePluginLoadingTest {

    @Test
    void newStylePluginLoadsViaLegacyAdapter() throws Exception {
        InstalledPlugin ip = new InstalledPlugin(null, NewStylePlugin.class.getName());

        // Touching getName() forces the info probe (collectPluginInfo →
        // startPluginImpl → instantiate → wrap → call initializePlugin →
        // call getPluginName).
        assertEquals("New-Style Test Plugin", ip.getName());
        assertEquals("Sample new-style plug-in for the loader test.", ip.getDescription());

        // The probe started the plug-in and stopped it again because the
        // type is reloadable. Start it directly to inspect what the loader
        // hands back.
        Plugin started = ip.startPlugin();
        assertInstanceOf(LegacyPluginAdapter.class, started);
        org.strangeeons.plugins.Plugin delegate =
                ((LegacyPluginAdapter) started).getDelegate();
        assertInstanceOf(NewStylePlugin.class, delegate);
        assertTrue(((NewStylePlugin) delegate).initialized,
                "new-style plug-in's initializePlugin must have run");
    }

    @Test
    void newStylePluginContextIsAdapted() throws Exception {
        InstalledPlugin ip = new InstalledPlugin(null, NewStylePlugin.class.getName());
        ip.startPlugin();

        Field delegateField = LegacyPluginAdapter.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);
        NewStylePlugin plugin = (NewStylePlugin) delegateField.get(ip.getPlugin());

        // The context the new-style plug-in saw must be adapted to the
        // new namespace, not the legacy type.
        assertInstanceOf(LegacyPluginContextAdapter.class, plugin.lastContext);
        // Settings flows through the adapter unchanged because Settings
        // already implements SettingsReadWrite.
        assertSame(plugin.lastContext.getSettings(), plugin.lastSettings);
    }

    /**
     * Sample plug-in that implements only the new namespace. Used by both
     * tests above.
     */
    public static final class NewStylePlugin extends AbstractPlugin {

        boolean initialized;
        PluginContext lastContext;
        org.strangeeons.plugins.SettingsReadWrite lastSettings;

        @Override
        public boolean initializePlugin(PluginContext context) {
            initialized = true;
            lastContext = context;
            lastSettings = context.getSettings();
            return true;
        }

        @Override
        public String getPluginName() {
            return "New-Style Test Plugin";
        }

        @Override
        public String getPluginDescription() {
            return "Sample new-style plug-in for the loader test.";
        }

        @Override
        public int getPluginType() {
            return ACTIVATED;
        }
    }
}
