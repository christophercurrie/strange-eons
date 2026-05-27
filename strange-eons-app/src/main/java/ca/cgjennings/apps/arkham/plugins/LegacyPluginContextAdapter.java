package ca.cgjennings.apps.arkham.plugins;

/**
 * Adapts a legacy {@link PluginContext} to the new
 * {@link org.strangeeons.plugins.PluginContext} interface so that new-style
 * plug-ins can receive any concrete legacy context. {@link PluginContextImpl}
 * already implements both interfaces directly; this adapter exists for
 * completeness and to insulate new-style plug-ins from the underlying
 * concrete type.
 */
final class LegacyPluginContextAdapter implements org.strangeeons.plugins.PluginContext {

    private final PluginContext delegate;

    LegacyPluginContextAdapter(PluginContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isInformationProbe() {
        return delegate.isInformationProbe();
    }

    @Override
    public org.strangeeons.plugins.SettingsReadWrite getSettings() {
        return delegate.getSettings();
    }
}
