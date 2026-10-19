package ca.cgjennings.ui;

/**
 * Converts mouse wheel rotation into zoom changes. Rotations combine
 * multiplicatively, so many small (trackpad) rotations have the same effect as
 * one large rotation of the same total.
 */
public final class WheelZoom {

    /** Zoom factor applied for each full unit of wheel rotation. */
    public static final double FACTOR = 1.25d;

    private WheelZoom() {
    }

    /**
     * Returns the zoom that results from applying a wheel rotation to the
     * current zoom.
     *
     * @param current the current zoom
     * @param rotation the precise wheel rotation; negative values zoom in
     * unless inverted
     * @param invert if {@code true}, positive rotations zoom in
     * @param min the minimum zoom
     * @param max the maximum zoom
     * @return the new zoom, clamped to {@code min..max}
     */
    public static double apply(double current, double rotation, boolean invert, double min, double max) {
        double exponent = invert ? rotation : -rotation;
        double zoom = current * Math.pow(FACTOR, exponent);
        return Math.max(min, Math.min(zoom, max));
    }
}
