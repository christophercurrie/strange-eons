package org.strangeeons.plugins;

import java.util.Set;

/**
 * Read access to a Strange Eons settings scope. Implemented by
 * {@code resources.Settings} and any future settings backings.
 *
 * <p>This Tier 1 surface is intentionally toolkit-agnostic: it exposes
 * only string-keyed primitive and string accessors. Typed accessors that
 * would force a reference to a specific UI toolkit (colours, regions,
 * images, fonts) will be added back in a toolkit-agnostic way as the
 * SPI's toolkit boundary is designed.
 *
 * @since 3.5
 */
public interface SettingsReader {

    /**
     * Returns the value of the named key, or {@code null} if no value is set
     * in this scope or any parent scope.
     */
    String get(String key);

    /**
     * Returns the value of the named key, or {@code defaultValue} if no value
     * is set in this scope or any parent scope.
     */
    String get(String key, String defaultValue);

    /**
     * Returns the value of the named key as text, with any leading or
     * trailing white space removed.
     */
    String getText(String key);

    /**
     * Returns the boolean value of the named key.
     */
    boolean getYesNo(String key);

    /**
     * Returns the integer value of the named key.
     */
    int getInt(String key);

    /**
     * Returns the {@code float} value of the named key.
     */
    float getFloat(String key);

    /**
     * Returns the {@code double} value of the named key.
     */
    double getDouble(String key);

    /**
     * Returns the keys defined directly in this scope.
     */
    Set<String> getKeySet();

    /**
     * Returns the keys visible in this scope, including inherited ones.
     */
    Set<String> getVisibleKeySet();

    /**
     * Returns the parent scope, or {@code null} if this scope has no parent.
     */
    SettingsReader getParent();
}
