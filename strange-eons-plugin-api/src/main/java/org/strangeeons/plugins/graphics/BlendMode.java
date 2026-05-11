package org.strangeeons.plugins.graphics;

/**
 * Pixel-level blending mode for the inner draws of a
 * {@link Graphics#withBlendMode(BlendMode, java.util.function.Consumer)}
 * scope. Names match the Java2D {@code AlphaComposite} family and the
 * Compose {@code BlendMode} enum; the framework translates to the
 * active backend.
 *
 * <p>The default mode (used outside any {@code withBlendMode} scope)
 * is {@link #SRC_OVER}, matching standard alpha compositing.
 *
 * @since 3.6
 */
public enum BlendMode {
    /** Standard alpha compositing: source over destination. */
    SRC_OVER,

    /** Source replaces destination; destination's alpha is ignored. */
    SRC,

    /** Destination is preserved; source is ignored. */
    DST,

    /** Source rendered only where destination is non-transparent. */
    SRC_IN,

    /** Destination preserved only where source is non-transparent. */
    DST_IN,

    /** Source rendered only where destination is transparent. */
    SRC_OUT,

    /** Destination preserved only where source is transparent. */
    DST_OUT,

    /** Source over destination, but only where destination is non-transparent. */
    SRC_ATOP,

    /** Destination over source, but only where source is non-transparent. */
    DST_ATOP,

    /** Cleared region: both source and destination become transparent. */
    CLEAR,

    /** Source XOR destination — non-overlapping union. */
    XOR,
}
