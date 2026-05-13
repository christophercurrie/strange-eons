/**
 * Game metadata SPI: the toolkit-neutral interfaces plug-ins use to
 * type-check their references to games, expansions, and class maps.
 *
 * <p>The interfaces in this package expose data accessors only.
 * Registration of new games, expansions, and class maps continues to
 * happen through the legacy {@code gamedata.*} static methods until
 * a dedicated registration SPI is designed. Plug-ins that already
 * call {@code gamedata.Game.register(...)} etc. keep doing so; the
 * returned values can be assigned to SPI-typed references because
 * the legacy concrete classes implement the SPI interfaces.
 *
 * @since 3.6
 */
package org.strangeeons.plugins.gamedata;
