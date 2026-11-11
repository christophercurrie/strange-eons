package org.strangeeons.testkit;

import ca.cgjennings.layout.MarkupRenderer;
import java.awt.RenderingHints;
import java.util.Map;
import org.strangeeons.core.settings.SettingValues;
import org.strangeeons.plugins.SettingsReader;

/**
 * Rendering setup that matches how the application exports sheets and sets up
 * markup renderers. Renderer options are read from the given settings, falling
 * back on the application's defaults.
 */
final class StandardRendering {

    /** The hints the application uses when exporting or printing. */
    static final Map<RenderingHints.Key, Object> EXPORT_HINTS = Map.of(
            RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY,
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON,
            RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY,
            RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE,
            RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON,
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC,
            RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY,
            RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE,
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    private StandardRendering() {
    }

    /**
     * Applies the options the application applies to every sheet renderer:
     * bad box highlighting, text scaling and spacing limits, and the default
     * text fitting method.
     */
    static void initialize(MarkupRenderer r, SettingsReader settings) {
        r.setMarkBadBox(SettingValues.yesNo(value(settings, "highlight-bad-boxes", "yes")));
        r.setScalingLimit(SettingValues.number(value(settings, "min-text-scaling-factor", "0.4")));
        r.setTightnessLimit((float) SettingValues.number(value(settings, "min-text-spacing-factor", "0.3")));
        r.setScalingFractionalLimit(SettingValues.integer(value(settings, "text-scaling-precision", "100")));
        r.setTextFitting(textFitting(settings.get("default-text-fitting")));
    }

    static int textFitting(String value) {
        if (value == null) {
            return MarkupRenderer.FIT_BOTH;
        }
        switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "none":
                return MarkupRenderer.FIT_NONE;
            case "spacing":
                return MarkupRenderer.FIT_TIGHTEN_LINE_SPACING;
            case "scaling":
                return MarkupRenderer.FIT_SCALE_TEXT;
            case "both":
                return MarkupRenderer.FIT_BOTH;
            default:
                throw new IllegalArgumentException("unknown text fitting method: " + value);
        }
    }

    private static String value(SettingsReader settings, String key, String defaultValue) {
        String v = settings.get(key);
        return v == null ? defaultValue : v;
    }
}
