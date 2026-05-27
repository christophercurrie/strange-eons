package org.strangeeons.plugins;

/**
 * Convenience base class for plug-ins built against the
 * {@code org.strangeeons.plugins} API. Provides default implementations
 * for lifecycle methods that have a sensible default; subclasses must
 * implement {@link #getPluginName()}, {@link #getPluginDescription()},
 * and {@link #getPluginType()}, plus
 * {@link #showPlugin(PluginContext, boolean)} for {@code ACTIVATED}
 * plug-ins that present a UI.
 *
 * @since 3.5
 */
public abstract class AbstractPlugin implements Plugin {

    /**
     * Default initialization — accepts the context and reports success.
     * Override to perform setup work.
     */
    @Override
    public boolean initializePlugin(PluginContext context) {
        return true;
    }

    /**
     * Default no-op. Override to release resources held by the plug-in.
     */
    @Override
    public void unloadPlugin() {
    }

    /**
     * Default version {@code 1.0f}. Override to report a different version.
     */
    @Override
    public float getPluginVersion() {
        return 1.0f;
    }

    /**
     * Default no-op. Override for {@code ACTIVATED} plug-ins to show or
     * hide the plug-in's interface.
     */
    @Override
    public void showPlugin(PluginContext context, boolean show) {
    }

    /**
     * Default {@code false}. Override for {@code ACTIVATED} plug-ins
     * that present a UI so Strange Eons can track the showing state.
     */
    @Override
    public boolean isPluginShowing() {
        return false;
    }

    /**
     * Default {@code true}. Override to disable the plug-in's menu item
     * conditionally (e.g. when the current component is not of the
     * expected type).
     */
    @Override
    public boolean isPluginUsable() {
        return true;
    }

    /**
     * Default {@code null} — no preferred accelerator. Override to
     * suggest a default keyboard shortcut.
     */
    @Override
    public String getDefaultAcceleratorKey() {
        return null;
    }
}
