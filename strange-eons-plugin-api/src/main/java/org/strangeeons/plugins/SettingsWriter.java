package org.strangeeons.plugins;

import java.io.IOException;
import org.strangeeons.plugins.graphics.Font;
import org.strangeeons.plugins.graphics.Paint;
import org.strangeeons.plugins.graphics.Region;

/**
 * Write access to a Strange Eons settings scope. Implemented by
 * {@code resources.Settings}.
 *
 * <p>This Tier 1 surface is intentionally toolkit-agnostic: it exposes
 * only string-keyed primitive and string mutators, plus default-impl
 * typed mutators for the SPI value types ({@link Region}, {@link Paint},
 * {@link Font}) that format the value to its canonical settings string
 * and call {@link #set(String, String)}.
 *
 * @since 3.5
 */
public interface SettingsWriter {

    /**
     * Sets the value of the named key in this scope.
     */
    void set(String key, String value);

    /**
     * Sets the trimmed text value of the named key.
     */
    void setText(String key, String value);

    /**
     * Sets the boolean value of the named key.
     */
    void setYesNo(String key, boolean value);

    /**
     * Sets the integer value of the named key.
     */
    void setInt(String key, int value);

    /**
     * Sets the {@code float} value of the named key.
     */
    void setFloat(String key, float value);

    /**
     * Sets the {@code double} value of the named key.
     */
    void setDouble(String key, double value);

    /**
     * Removes any value set directly in this scope for the named key.
     * Inherited values from parent scopes remain visible.
     */
    void reset(String key);

    /**
     * Loads settings from the named resource, adding them to this scope.
     */
    void addSettingsFrom(String resource) throws IOException;

    // -- Typed SPI value mutators (default impls format via type factories) --

    /**
     * Writes {@code region} to {@code key} using its canonical
     * settings-format string {@code "x,y,width,height"}. Passing
     * {@code null} clears the value (equivalent to {@link #reset(String)}).
     */
    default void writeRegion(String key, Region region) {
        if (region == null) {
            reset(key);
        } else {
            set(key, region.toSetting());
        }
    }

    /**
     * Writes {@code color} to {@code key} using its canonical
     * settings-format hex string. Passing {@code null} clears the
     * value.
     */
    default void writeColor(String key, Paint color) {
        if (color == null) {
            reset(key);
            return;
        }
        switch (color) {
            case Paint.Color c -> set(key, c.getHex());
        }
    }

    /**
     * Writes {@code font} to {@code key} using its canonical
     * settings-format string {@code "family,size,style"}. Passing
     * {@code null} clears the value.
     */
    default void writeFont(String key, Font font) {
        if (font == null) {
            reset(key);
        } else {
            set(key, font.toSetting());
        }
    }
}
