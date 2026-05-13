package gamedata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the legacy {@code gamedata.*} classes implement their
 * corresponding {@code org.strangeeons.plugins.gamedata.*} SPI
 * interfaces.
 */
class GamedataSpiTest {

    @Test
    void legacyGameImplementsSpiGame() {
        Game legacy = Game.register("SPI_TEST_GAME", "SPI Test Game");
        org.strangeeons.plugins.gamedata.Game spi = legacy;
        assertSame(legacy, spi);

        assertEquals("SPI_TEST_GAME", spi.getCode());
        assertNotNull(spi.getUIName());
        assertNotNull(spi.getGameName());
        assertNotNull(spi.getSettings());
    }

    @Test
    void gameSettingKeyMatchesLegacyValue() {
        assertEquals(Game.GAME_SETTING_KEY,
                org.strangeeons.plugins.gamedata.Game.GAME_SETTING_KEY);
    }

    @Test
    void legacyExpansionImplementsSpiExpansion() {
        Game game = Game.register("SPI_TEST_EXP_GAME", "Expansion-Owning Game");
        Expansion legacy = Expansion.register(
                game, "EXP", "Expansion Display", "expansion-internal",
                (String) null, (java.awt.image.BufferedImage[]) null);
        org.strangeeons.plugins.gamedata.Expansion spi = legacy;
        assertSame(legacy, spi);

        assertEquals("EXP", spi.getCode());
        assertNotNull(spi.getUIName());
        assertNotNull(spi.getGameName());

        org.strangeeons.plugins.gamedata.Game spiGame = spi.getGame();
        assertSame(game, spiGame);
    }

    @Test
    void expansionSettingKeysMatchLegacyValues() {
        assertEquals(Expansion.EXPANSION_SETTING_KEY,
                org.strangeeons.plugins.gamedata.Expansion.EXPANSION_SETTING_KEY);
        assertEquals(Expansion.VARIANT_SETTING_KEY,
                org.strangeeons.plugins.gamedata.Expansion.VARIANT_SETTING_KEY);
    }

    @Test
    void legacyClassMapImplementsSpiClassMap() throws Exception {
        // ClassMap is a marker interface today; just verify the
        // type relationship exists. Constructing a real ClassMap
        // requires a resource on the classpath; instead, we verify
        // the runtime class implements the SPI interface.
        assertSame(true,
                org.strangeeons.plugins.gamedata.ClassMap.class
                        .isAssignableFrom(ClassMap.class));
    }
}
