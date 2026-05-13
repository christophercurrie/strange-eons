package org.strangeeons.plugins.gamedata;

import org.strangeeons.plugins.SettingsReadWrite;

/**
 * A registered game — the top-level grouping that game components
 * belong to (e.g. a specific card game, board game, or RPG system).
 * Each game has a unique code, a display name, an internal name
 * used by game-data files, and an associated settings scope that
 * plug-ins can read and write through.
 *
 * <p>Games are registered through the legacy static
 * {@code gamedata.Game.register(...)} methods; the returned
 * instance implements this SPI interface so plug-ins can hold a
 * reference of this type. A dedicated registration SPI is deferred.
 *
 * @since 3.6
 */
public interface Game {

    /**
     * The setting key under which a game component's owning game
     * code is recorded.
     */
    String GAME_SETTING_KEY = "game";

    /** Returns the game's unique code (e.g. {@code "MTG"}). */
    String getCode();

    /** Returns the display name shown in user interfaces. */
    String getUIName();

    /**
     * Returns the internal game-data name (typically the lowercase
     * form used in resource paths and game-data files).
     */
    String getGameName();

    /**
     * Returns the game's settings scope. Reads fall back through
     * parent scopes; writes affect only this scope and persist for
     * the lifetime of the game's registration.
     */
    SettingsReadWrite getSettings();
}
