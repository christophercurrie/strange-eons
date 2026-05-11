package org.strangeeons.plugins.graphics;

/**
 * A fill paint for shape primitives. {@code Paint} is a sealed
 * hierarchy of concrete paint kinds: plug-ins construct instances
 * via the static factory methods on this interface, and the
 * framework pattern-matches on the nested record types when
 * rendering. Plug-ins may also pattern-match if they need to
 * introspect a paint they were handed.
 *
 * @since 3.6
 */
public sealed interface Paint {

    /**
     * A solid-color paint described by an {@code "RRGGBB"} or
     * {@code "AARRGGBB"} hexadecimal string. The framework parses
     * the string when rendering.
     */
    record Color(String hex) implements Paint {
        /** Bean-style getter for {@link #hex()}. */
        public String getHex() { return hex; }
    }

    /**
     * Creates a solid-color paint from a hex string in either the
     * {@code "RRGGBB"} or {@code "AARRGGBB"} format.
     */
    static Paint color(String hex) {
        return new Color(hex);
    }
}
