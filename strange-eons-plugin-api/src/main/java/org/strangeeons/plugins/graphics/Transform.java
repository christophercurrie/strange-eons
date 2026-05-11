package org.strangeeons.plugins.graphics;

/**
 * A 2D affine transform applied via
 * {@link Graphics#withTransform(Transform, java.util.function.Consumer)}.
 * {@code Transform} is a sealed hierarchy with a single concrete
 * shape today; transforms compose via {@link #compose(Transform)}
 * and are applied in the order they appear in source code.
 *
 * <p>The six matrix components use the same names as
 * {@link java.awt.geom.AffineTransform}: {@code scaleX},
 * {@code shearX}, {@code translateX}, {@code shearY},
 * {@code scaleY}, {@code translateY}. The augmented 3×3 matrix is:
 *
 * <pre>
 *   [ scaleX  shearX  translateX ]
 *   [ shearY  scaleY  translateY ]
 *   [    0       0         1     ]
 * </pre>
 *
 * @since 3.6
 */
public sealed interface Transform {

    /** Returns the {@code scaleX} component (matrix m00). */
    double getScaleX();

    /** Returns the {@code shearX} component (matrix m01). */
    double getShearX();

    /** Returns the {@code translateX} component (matrix m02). */
    double getTranslateX();

    /** Returns the {@code shearY} component (matrix m10). */
    double getShearY();

    /** Returns the {@code scaleY} component (matrix m11). */
    double getScaleY();

    /** Returns the {@code translateY} component (matrix m12). */
    double getTranslateY();

    /**
     * Returns a new transform that applies this transform first,
     * then the given transform.
     */
    Transform compose(Transform other);

    /**
     * A 2D affine transform with the six components named to match
     * {@link java.awt.geom.AffineTransform}: {@code scaleX},
     * {@code shearX}, {@code translateX} on the first row, and
     * {@code shearY}, {@code scaleY}, {@code translateY} on the
     * second row.
     */
    record Affine(double scaleX, double shearX, double translateX,
                  double shearY, double scaleY, double translateY) implements Transform {

        @Override public double getScaleX()     { return scaleX; }
        @Override public double getShearX()     { return shearX; }
        @Override public double getTranslateX() { return translateX; }
        @Override public double getShearY()     { return shearY; }
        @Override public double getScaleY()     { return scaleY; }
        @Override public double getTranslateY() { return translateY; }

        @Override
        public Transform compose(Transform other) {
            Affine o = (Affine) other;
            return new Affine(
                    scaleX * o.scaleX + shearX * o.shearY,
                    scaleX * o.shearX + shearX * o.scaleY,
                    scaleX * o.translateX + shearX * o.translateY + translateX,
                    shearY * o.scaleX + scaleY * o.shearY,
                    shearY * o.shearX + scaleY * o.scaleY,
                    shearY * o.translateX + scaleY * o.translateY + translateY);
        }
    }

    /** Returns the identity transform. */
    static Transform identity() {
        return IDENTITY;
    }

    /** Returns a translation by ({@code dx}, {@code dy}). */
    static Transform translate(double dx, double dy) {
        return new Affine(
                1, 0, dx,
                0, 1, dy);
    }

    /** Returns a rotation by {@code degrees} around the origin. */
    static Transform rotate(double degrees) {
        double r = Math.toRadians(degrees);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        return new Affine(
                cos, -sin, 0,
                sin,  cos, 0);
    }

    /**
     * Returns a rotation by {@code degrees} around the given center
     * point.
     */
    static Transform rotate(double degrees, double cx, double cy) {
        return translate(cx, cy)
                .compose(rotate(degrees))
                .compose(translate(-cx, -cy));
    }

    /** Returns a uniform scale by {@code factor}. */
    static Transform scale(double factor) {
        return new Affine(
                factor, 0, 0,
                     0, factor, 0);
    }

    /** Returns a scale by ({@code sx}, {@code sy}). */
    static Transform scale(double sx, double sy) {
        return new Affine(
                sx, 0, 0,
                 0, sy, 0);
    }

    Affine IDENTITY = new Affine(
            1, 0, 0,
            0, 1, 0);
}
