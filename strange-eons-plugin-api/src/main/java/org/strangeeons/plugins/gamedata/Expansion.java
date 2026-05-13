package org.strangeeons.plugins.gamedata;

/**
 * A registered expansion within a {@link Game} — a thematic
 * grouping of components released together (a card set, a rulebook
 * supplement, etc.). Each expansion has a unique code, a display
 * name, an internal name used by game-data files, and a back-
 * reference to the {@link Game} it belongs to.
 *
 * <p>Expansions are registered through the legacy static
 * {@code gamedata.Expansion.register(...)} methods; the returned
 * instance implements this SPI interface so plug-ins can hold a
 * reference of this type. A dedicated registration SPI is deferred.
 *
 * @since 3.6
 */
public interface Expansion {

    /**
     * Setting key under which a game component's active expansion
     * is recorded.
     */
    String EXPANSION_SETTING_KEY = "active-expansion";

    /**
     * Setting key under which a game component's active expansion-
     * symbol variant is recorded.
     */
    String VARIANT_SETTING_KEY = "active-variant";

    /** Returns the expansion's unique code (within its game). */
    String getCode();

    /** Returns the {@link Game} this expansion belongs to. */
    Game getGame();

    /** Returns the display name shown in user interfaces. */
    String getUIName();

    /**
     * Returns the internal game-data name (typically used in
     * resource paths).
     */
    String getGameName();
}
