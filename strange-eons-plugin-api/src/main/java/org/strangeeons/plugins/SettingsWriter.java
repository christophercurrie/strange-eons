package org.strangeeons.plugins;

import java.io.IOException;

/**
 * Write access to a Strange Eons settings scope. Implemented by
 * {@code resources.Settings}.
 *
 * <p>This Tier 1 surface is intentionally toolkit-agnostic: it exposes
 * only string-keyed primitive and string mutators. Typed mutators (e.g.
 * for colours and regions) will be added back in a toolkit-agnostic way
 * as the SPI's toolkit boundary is designed.
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
}
