package org.strangeeons.plugins;

import java.util.Set;
import org.strangeeons.plugins.graphics.Font;
import org.strangeeons.plugins.graphics.Paint;
import org.strangeeons.plugins.graphics.Region;

/**
 * Read access to a Strange Eons settings scope. Implemented by
 * {@code resources.Settings} and any future settings backings.
 *
 * <p>This Tier 1 surface is intentionally toolkit-agnostic: it exposes
 * only string-keyed primitive and string accessors. Typed accessors
 * for the SPI value types ({@link Region}, {@link Paint},
 * {@link Font}) are provided as default methods that parse the
 * canonical settings-format string returned by {@link #get(String)}.
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

    // -- Typed SPI value accessors (default impls parse via type factories) --

    /**
     * Returns the value of {@code key} parsed as a {@link Region}, or
     * {@code null} if no value is set.
     *
     * @throws IllegalArgumentException if the value is not a valid region
     */
    default Region readRegion(String key) {
        String value = get(key);
        return value == null ? null : Region.parse(value);
    }

    /**
     * Returns the value of {@code key} parsed as a {@link Region}, or
     * {@code defaultValue} if no value is set.
     */
    default Region readRegion(String key, Region defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Region.parse(value);
    }

    /**
     * Returns the value of {@code key} parsed as a {@link Paint.Color}, or
     * {@code null} if no value is set.
     */
    default Paint.Color readColor(String key) {
        String value = get(key);
        return value == null ? null : new Paint.Color(value);
    }

    /**
     * Returns the value of {@code key} parsed as a {@link Paint.Color}, or
     * {@code defaultValue} if no value is set.
     */
    default Paint.Color readColor(String key, Paint.Color defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : new Paint.Color(value);
    }

    /**
     * Returns the value of {@code key} parsed as a {@link Font}, or
     * {@code null} if no value is set.
     *
     * @throws IllegalArgumentException if the value is not a valid font
     */
    default Font readFont(String key) {
        String value = get(key);
        return value == null ? null : Font.parse(value);
    }

    /**
     * Returns the value of {@code key} parsed as a {@link Font}, or
     * {@code defaultValue} if no value is set.
     */
    default Font readFont(String key, Font defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Font.parse(value);
    }
}
