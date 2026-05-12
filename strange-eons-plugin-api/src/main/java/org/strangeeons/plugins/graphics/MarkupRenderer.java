package org.strangeeons.plugins.graphics;

/**
 * Lays out and renders marked-up text within a rectangular or
 * shaped region. Plug-ins typically configure a renderer with a
 * markup string, a default style, and per-tag style overrides, then
 * call {@link #draw(Graphics, Region)} to render the laid-out text
 * into a {@link Graphics}.
 *
 * <p>This is the toolkit-neutral SPI surface; the framework supplies
 * the concrete implementation. Plug-ins receive renderer instances
 * from the framework's layout helpers (or construct them through
 * framework factories) rather than instantiating an implementation
 * directly.
 *
 * @since 3.6
 */
public interface MarkupRenderer {

    // -- Alignment flags -----------------------------------------------------

    /** Horizontal alignment: align text to the left edge of the region. */
    int LAYOUT_LEFT    = 1;
    /** Horizontal alignment: center text horizontally within the region. */
    int LAYOUT_CENTER  = 2;
    /** Horizontal alignment: align text to the right edge of the region. */
    int LAYOUT_RIGHT   = 4;
    /** Vertical alignment: align text to the top edge of the region. */
    int LAYOUT_TOP     = 8;
    /** Vertical alignment: center text vertically within the region. */
    int LAYOUT_MIDDLE  = 16;
    /** Vertical alignment: align text to the bottom edge of the region. */
    int LAYOUT_BOTTOM  = 32;
    /** Horizontal alignment modifier: justify text to fill each line. */
    int LAYOUT_JUSTIFY = 64;

    // -- Text-fitting flags --------------------------------------------------

    /** Text-fitting mode: no adjustments — text may overflow. */
    int FIT_NONE                 = 0;
    /** Text-fitting mode: tighten line spacing before scaling. */
    int FIT_TIGHTEN_LINE_SPACING = 1;
    /** Text-fitting mode: scale text size to fit the region. */
    int FIT_SCALE_TEXT           = 2;
    /** Text-fitting mode: combine line-spacing tightening and text scaling. */
    int FIT_BOTH                 = 3;

    // -- Markup --------------------------------------------------------------

    /** Sets the markup text to be rendered. */
    void setMarkupText(String markup);

    /** Returns the markup text most recently set. */
    String getMarkupText();

    // -- Styling -------------------------------------------------------------

    /** Sets the default text style applied where no tag overrides it. */
    void setDefaultStyle(TextStyle style);

    /** Returns the default text style. */
    TextStyle getDefaultStyle();

    /**
     * Sets the text style applied to runs marked with the given
     * tag. Replaces any prior style or replacement for that tag.
     */
    void setStyleForTag(String tag, TextStyle style);

    // -- Line spacing --------------------------------------------------------

    /**
     * Sets the line-tightness factor (a relative scale applied to
     * the natural inter-line spacing). Values below 1.0 squeeze
     * lines closer together; values above 1.0 expand them.
     */
    void setLineTightness(float tightness);

    /** Returns the current line-tightness factor. */
    float getLineTightness();

    // -- Alignment / fit -----------------------------------------------------

    /**
     * Sets the alignment flags (a bitwise OR of one horizontal flag,
     * one vertical flag, and optionally {@link #LAYOUT_JUSTIFY}).
     */
    void setAlignment(int alignment);

    /** Returns the alignment flags. */
    int getAlignment();

    /**
     * Sets the text-fitting mode (one of {@link #FIT_NONE},
     * {@link #FIT_TIGHTEN_LINE_SPACING}, {@link #FIT_SCALE_TEXT},
     * {@link #FIT_BOTH}).
     */
    void setTextFitting(int mode);

    /** Returns the text-fitting mode. */
    int getTextFitting();

    // -- Shape ---------------------------------------------------------------

    /**
     * Constrains layout to a non-rectangular shape. Passing
     * {@code null} clears any previously set shape, restoring
     * rectangular layout.
     */
    void setPageShape(PageShape shape);

    /** Returns the active page shape, or {@code null} for rectangular layout. */
    PageShape getPageShape();

    // -- Rendering -----------------------------------------------------------

    /**
     * Lays out the markup text and renders it into the given region
     * on {@code g}. Returns the height in pixels actually consumed,
     * which may differ from the region height when text-fitting is
     * active.
     */
    double draw(Graphics g, Region region);

    /**
     * Lays out the markup text as a single line and renders it into
     * the given region, clipping or compressing as needed. Returns
     * the laid-out width.
     */
    double drawAsSingleLine(Graphics g, Region region);

    /**
     * Lays out the markup text as if it were to be rendered into the
     * given region, but does not actually draw anything. Returns the
     * height in pixels the laid-out text would consume.
     */
    double measure(Graphics g, Region region);
}
