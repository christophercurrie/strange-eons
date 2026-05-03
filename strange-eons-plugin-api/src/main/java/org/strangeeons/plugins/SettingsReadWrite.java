package org.strangeeons.plugins;

/**
 * Read-and-write access to a Strange Eons settings scope. The natural
 * type for {@link PluginContext#getSettings()}.
 *
 * @since 3.5
 */
public interface SettingsReadWrite extends SettingsReader, SettingsWriter {
}
