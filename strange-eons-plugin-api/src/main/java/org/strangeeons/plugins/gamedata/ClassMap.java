package org.strangeeons.plugins.gamedata;

/**
 * A registry of game-component classes, populated from class-map
 * resources ({@code .classmap} files) declared by plug-ins.
 *
 * <p>Class maps are fundamentally a registration mechanism: plug-ins
 * register their class-map resources through the legacy
 * {@code gamedata.ClassMap.add(String)} static method, and the
 * framework consumes the resulting registry to populate the
 * "New Component" UI and to resolve class identifiers when loading
 * saved components.
 *
 * <p>This SPI interface is currently a marker that lets plug-ins
 * type-check their references against the toolkit-neutral surface;
 * the data methods and registration helpers will be added when a
 * dedicated registration SPI is designed.
 *
 * @since 3.6
 */
public interface ClassMap {
}
