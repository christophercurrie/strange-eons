package org.strangeeons.plugins;

/**
 * The runtime context passed to a plug-in during initialization, show, and
 * hide. A pure interface implemented by Strange Eons' internal context
 * type.
 *
 * <p>This Tier 1 surface is deliberately minimal: it exposes only the
 * information probe flag and the plug-in's settings scope. Activation
 * modifier keys, the host plug-in instance, and other context affordances
 * will be added back in a toolkit-agnostic way as the SPI's toolkit
 * boundary is designed.
 *
 * @since 3.5
 */
public interface PluginContext {

    /**
     * Returns whether this context was created solely to query the plug-in
     * for its name, version, description, and type. A plug-in may use this
     * as a hint to skip optional initialization work.
     */
    boolean isInformationProbe();

    /**
     * Returns a settings scope namespaced to this plug-in. Reads fall back
     * through parent scopes; writes affect only this scope and persist
     * between application runs.
     */
    SettingsReadWrite getSettings();
}
