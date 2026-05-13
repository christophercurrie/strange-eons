package org.strangeeons.plugins;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.strangeeons.plugins.graphics.Font;
import org.strangeeons.plugins.graphics.Paint;
import org.strangeeons.plugins.graphics.Region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the default-method typed accessors on
 * {@link SettingsReader} and {@link SettingsWriter} parse and format
 * SPI value types via {@link SettingsReader#get(String)} /
 * {@link SettingsWriter#set(String, String)}.
 */
class SettingsValueAccessorsTest {

    @Test
    void readRegionParsesCanonicalFormat() {
        var s = new MapSettings();
        s.set("name-region", "14,17,212,40");
        Region r = s.readRegion("name-region");
        assertEquals(14, r.getX());
        assertEquals(17, r.getY());
        assertEquals(212, r.getWidth());
        assertEquals(40, r.getHeight());
    }

    @Test
    void readRegionReturnsNullWhenMissing() {
        var s = new MapSettings();
        assertNull(s.readRegion("absent"));
    }

    @Test
    void readRegionWithDefaultReturnsDefault() {
        var s = new MapSettings();
        Region fallback = new Region(0, 0, 1, 1);
        assertEquals(fallback, s.readRegion("absent", fallback));
    }

    @Test
    void readRegionThrowsOnMalformedValue() {
        var s = new MapSettings();
        s.set("bad", "not,a,region");
        assertThrows(IllegalArgumentException.class,
                () -> s.readRegion("bad"));
    }

    @Test
    void writeRegionFormatsCanonicalString() {
        var s = new MapSettings();
        s.writeRegion("rect", new Region(10, 20, 30, 40));
        assertEquals("10,20,30,40", s.get("rect"));
    }

    @Test
    void writeRegionWithNullClearsValue() {
        var s = new MapSettings();
        s.set("rect", "1,2,3,4");
        s.writeRegion("rect", null);
        assertNull(s.get("rect"));
    }

    @Test
    void readColorReturnsHexBackedColor() {
        var s = new MapSettings();
        s.set("frame-color", "FF8800");
        Paint.Color c = s.readColor("frame-color");
        assertEquals("FF8800", c.getHex());
    }

    @Test
    void readColorWithDefaultReturnsDefault() {
        var s = new MapSettings();
        Paint.Color fallback = new Paint.Color("000000");
        assertEquals(fallback, s.readColor("absent", fallback));
    }

    @Test
    void writeColorFormatsHex() {
        var s = new MapSettings();
        s.writeColor("body-text", new Paint.Color("AARRGGBB".toLowerCase()));
        assertEquals("aarrggbb", s.get("body-text"));
    }

    @Test
    void readFontParsesCanonicalFormat() {
        var s = new MapSettings();
        s.set("title-font", "Dialog,18.0,bold");
        Font f = s.readFont("title-font");
        assertEquals("Dialog", f.getFamily());
        assertEquals(18.0, f.getSize());
        assertEquals(Font.Style.BOLD, f.getStyle());
    }

    @Test
    void writeFontFormatsCanonicalString() {
        var s = new MapSettings();
        s.writeFont("title-font", new Font("SansSerif", 14.0, Font.Style.ITALIC));
        assertEquals("SansSerif,14.0,italic", s.get("title-font"));
    }

    // ----------------------------------------------------------------------

    /** Minimal in-memory SettingsReadWrite for testing default-method behavior. */
    private static final class MapSettings implements SettingsReadWrite {
        private final Map<String, String> map = new HashMap<>();

        @Override public String get(String key) { return map.get(key); }
        @Override public String get(String key, String defaultValue) {
            return map.getOrDefault(key, defaultValue);
        }
        @Override public String getText(String key) {
            String v = map.get(key);
            return v == null ? null : v.strip();
        }
        @Override public boolean getYesNo(String key) {
            return "yes".equalsIgnoreCase(map.get(key));
        }
        @Override public int getInt(String key) {
            return Integer.parseInt(map.get(key));
        }
        @Override public float getFloat(String key) {
            return Float.parseFloat(map.get(key));
        }
        @Override public double getDouble(String key) {
            return Double.parseDouble(map.get(key));
        }
        @Override public Set<String> getKeySet() { return map.keySet(); }
        @Override public Set<String> getVisibleKeySet() { return map.keySet(); }
        @Override public SettingsReader getParent() { return null; }

        @Override public void set(String key, String value) { map.put(key, value); }
        @Override public void setText(String key, String value) {
            map.put(key, value == null ? null : value.strip());
        }
        @Override public void setYesNo(String key, boolean value) {
            map.put(key, value ? "yes" : "no");
        }
        @Override public void setInt(String key, int value) {
            map.put(key, Integer.toString(value));
        }
        @Override public void setFloat(String key, float value) {
            map.put(key, Float.toString(value));
        }
        @Override public void setDouble(String key, double value) {
            map.put(key, Double.toString(value));
        }
        @Override public void reset(String key) { map.remove(key); }
        @Override public void addSettingsFrom(String resource) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
