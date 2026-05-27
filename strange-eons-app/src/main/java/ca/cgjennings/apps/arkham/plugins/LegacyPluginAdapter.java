package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.ui.theme.ThemedIcon;

/**
 * Wraps an {@link org.strangeeons.plugins.Plugin} (new-namespace plug-in)
 * and exposes it as a legacy {@link Plugin} so the application's existing
 * call sites can drive new-style plug-ins without branching. The plug-in
 * loader installs an instance of this adapter when it discovers a class
 * that implements only the new interface.
 *
 * <p>Lifecycle methods that take a {@link PluginContext} wrap the legacy
 * context in a {@link LegacyPluginContextAdapter} before delegating, so
 * the wrapped plug-in only ever sees the new namespace's types.
 *
 * <p>Methods on the legacy interface that have no new-namespace equivalent
 * yet — currently {@link #getPluginIcon()} (deferred until a
 * toolkit-agnostic icon abstraction is added) — return defaults that the
 * surrounding code already knows how to handle.
 */
final class LegacyPluginAdapter implements Plugin {

    private final org.strangeeons.plugins.Plugin delegate;

    LegacyPluginAdapter(org.strangeeons.plugins.Plugin delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the wrapped new-namespace plug-in instance.
     */
    org.strangeeons.plugins.Plugin getDelegate() {
        return delegate;
    }

    @Override
    public boolean initializePlugin(PluginContext context) {
        return delegate.initializePlugin(new LegacyPluginContextAdapter(context));
    }

    @Override
    public void unloadPlugin() {
        delegate.unloadPlugin();
    }

    @Override
    public String getPluginName() {
        return delegate.getPluginName();
    }

    @Override
    public String getPluginDescription() {
        return delegate.getPluginDescription();
    }

    @Override
    public float getPluginVersion() {
        return delegate.getPluginVersion();
    }

    @Override
    public int getPluginType() {
        return delegate.getPluginType();
    }

    @Override
    public void showPlugin(PluginContext context, boolean show) {
        delegate.showPlugin(new LegacyPluginContextAdapter(context), show);
    }

    @Override
    public boolean isPluginShowing() {
        return delegate.isPluginShowing();
    }

    @Override
    public boolean isPluginUsable() {
        return delegate.isPluginUsable();
    }

    @Override
    public ThemedIcon getPluginIcon() {
        return null;
    }

    @Override
    public String getDefaultAcceleratorKey() {
        return delegate.getDefaultAcceleratorKey();
    }
}
