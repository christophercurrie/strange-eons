package org.strangeeons.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.layout.PageShape;
import ca.cgjennings.layout.TextStyle;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.strangeeons.core.Host;
import org.strangeeons.core.graphics.awt.AwtImageHandle;
import org.strangeeons.plugins.graphics.Font;
import org.strangeeons.plugins.graphics.Paint;
import org.strangeeons.plugins.graphics.Region;

class TestSettingsTest {

    @TempDir
    Path tmp;

    @Test
    void fallsBackOnTheParent() {
        TestSettings parent = TestSettings.of(Map.of("a", "parent", "b", "parent"));
        TestSettings child = new TestSettings(parent);
        child.set("a", "child");
        assertEquals("child", child.get("a"));
        assertEquals("parent", child.get("b"));
        assertNull(child.get("c"));
        assertEquals("def", child.get("c", "def"));
        assertSame(parent, child.getParent());

        child.set("b", null);
        assertNull(child.get("b"));
        child.reset("b");
        assertEquals("parent", child.get("b"));

        assertEquals(Set.of("a"), child.getKeySet());
        assertEquals(Set.of("a", "b"), child.getVisibleKeySet());
        assertEquals(Set.of("a", "b"), parent.getVisibleKeySet());
        assertThrows(NullPointerException.class, () -> child.get(null));
        assertThrows(NullPointerException.class, () -> child.set(null, "x"));
        assertThrows(NullPointerException.class, () -> child.reset(null));
        assertTrue(child.toString().contains("a=child"));
    }

    @Test
    void readsAndWritesTypedValues() {
        TestSettings s = new TestSettings();
        s.setYesNo("flag", true);
        s.setInt("count", 7);
        s.setFloat("scale", 1.5f);
        s.setDouble("ratio", 0.25);
        s.setText("title", "Hello");
        assertEquals("yes", s.get("flag"));
        assertTrue(s.getYesNo("flag"));
        assertFalse(s.getYesNo("missing"));
        assertEquals(7, s.getInt("count"));
        assertEquals(1.5f, s.getFloat("scale"));
        assertEquals(0.25, s.getDouble("ratio"));
        assertEquals("Hello", s.getText("title"));
        assertEquals("Hello", s.getText("title-text"));
        assertEquals("[MISSING: subtitle]", s.getText("subtitle"));

        s.set("bad", "seven");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> s.getInt("bad"));
        assertTrue(e.getMessage().contains("bad = seven"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> s.getDouble("missing"));
    }

    @Test
    void readsSpiValueTypes() {
        TestSettings s = new TestSettings();
        s.writeRegion("box", new Region(1, 2, 3, 4));
        s.writeColor("ink", Paint.color("ff8800"));
        s.writeFont("title", Font.parse("Serif,12,BOLD"));
        assertEquals(new Region(1, 2, 3, 4), s.readRegion("box"));
        assertEquals("ff8800", s.readColor("ink").getHex());
        assertEquals("Serif", s.readFont("title").getFamily());
        s.writeRegion("box", null);
        assertNull(s.readRegion("box"));
    }

    @Test
    void readsStylesShapesAndAlignment() {
        TestSettings s = TestSettings.of(Map.of(
                "body-style", "family: 'Serif'; size: 10",
                "body-shape", "1, 2, 3, 4, 5",
                "body-alignment", "center, middle",
                "bad-style", "bogus: 1"));
        TextStyle base = new TextStyle(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        assertSame(base, s.readTextStyle("body", base));
        assertEquals("Serif", base.get(TextAttribute.FAMILY));
        assertEquals(TextAttribute.WEIGHT_BOLD, base.get(TextAttribute.WEIGHT));
        assertNotNull(s.readTextStyle("body-style", null));
        TextStyle other = new TextStyle();
        assertSame(other, s.readTextStyle("none", other));
        assertThrows(IllegalArgumentException.class, () -> s.readTextStyle("bad", null));
        assertThrows(IllegalArgumentException.class, () -> s.readTextStyle("body", new org.strangeeons.plugins.graphics.TextStyle() {
            @Override
            public String getFamily() {
                return null;
            }

            @Override
            public void setFamily(String family) {
            }
        }));

        assertTrue(s.readPageShape("body") instanceof PageShape.CupShape);
        assertNull(s.readPageShape("none"));
        assertEquals(MarkupRenderer.LAYOUT_CENTER | MarkupRenderer.LAYOUT_MIDDLE, s.readAlignment("body"));
        assertEquals(MarkupRenderer.LAYOUT_LEFT | MarkupRenderer.LAYOUT_TOP, s.readAlignment("none"));
    }

    @Test
    void loadsSettingsResources() throws IOException {
        Path res = tmp.resolve("plugin/resources");
        Files.createDirectories(res.resolve("game"));
        Files.writeString(res.resolve("game/card.settings"), "name-region = 10,20,30,40\ntitle = A\\: B\n");
        try (TestResources r = TestResources.install(res)) {
            TestSettings s = new TestSettings();
            s.addSettingsFrom("game/card.settings");
            s.addSettingsFrom("");
            assertEquals(new Region(10, 20, 30, 40), s.readRegion("name-region"));
            assertEquals("A: B", s.get("title"));
            assertThrows(FileNotFoundException.class, () -> s.addSettingsFrom("game/none.settings"));
            assertThrows(NullPointerException.class, () -> s.addSettingsFrom(null));
        }
    }

    @Test
    void readsImagesThroughResourcesWithAStandIn() throws IOException {
        Path res = tmp.resolve("images/resources");
        Files.createDirectories(res);
        ImageIO.write(new BufferedImage(5, 3, BufferedImage.TYPE_INT_RGB), "png", res.resolve("art.png").toFile());
        Files.writeString(res.resolve("broken.png"), "not an image");
        try (TestResources r = TestResources.install(res)) {
            TestSettings s = TestSettings.of(Map.of("art", "art.png", "gone", "gone.png", "broken", "broken.png"));
            AwtImageHandle art = (AwtImageHandle) s.readImage("art");
            assertEquals(5, art.getWidth());
            assertEquals(Host.get().getMissingImage().getWidth(), s.readImage("gone").getWidth());
            assertEquals(Host.get().getMissingImage().getWidth(), s.readImage("broken").getWidth());
            assertNull(s.readImage("none"));
        }
    }
}
