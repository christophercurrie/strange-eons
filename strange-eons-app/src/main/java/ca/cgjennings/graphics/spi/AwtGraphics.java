package ca.cgjennings.graphics.spi;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.function.Consumer;
import org.strangeeons.plugins.graphics.BlendMode;
import org.strangeeons.plugins.graphics.Font;
import org.strangeeons.plugins.graphics.FontMetrics;
import org.strangeeons.plugins.graphics.Graphics;
import org.strangeeons.plugins.graphics.ImageHandle;
import org.strangeeons.plugins.graphics.Paint;
import org.strangeeons.plugins.graphics.Region;
import org.strangeeons.plugins.graphics.Shape;
import org.strangeeons.plugins.graphics.Stroke;
import org.strangeeons.plugins.graphics.Transform;
import resources.Settings;

/**
 * An AWT-backed {@link Graphics} implementation. Delegates each SPI
 * call to the wrapped {@link Graphics2D}, translating SPI value
 * types (paint, stroke, font, transform, blend mode) to their AWT
 * equivalents at the call site.
 *
 * <p>Plug-ins do not construct {@code AwtGraphics} directly; the
 * framework wraps the {@code Graphics2D} produced by a sheet and
 * passes the wrapper to the active {@code ComponentHandler}.
 */
public final class AwtGraphics implements Graphics {

    private final Graphics2D g2;

    public AwtGraphics(Graphics2D g2) {
        this.g2 = Objects.requireNonNull(g2, "g2");
    }

    // -- Shape primitives ----------------------------------------------------

    @Override
    public void fillRect(double x, double y, double w, double h, Paint paint) {
        g2.setPaint(toAwtPaint(paint));
        g2.fill(new Rectangle2D.Double(x, y, w, h));
    }

    @Override
    public void drawRect(double x, double y, double w, double h, Stroke stroke) {
        applyStroke(stroke);
        g2.draw(new Rectangle2D.Double(x, y, w, h));
    }

    @Override
    public void fillRoundRect(double x, double y, double w, double h,
                              double arcWidth, double arcHeight, Paint paint) {
        g2.setPaint(toAwtPaint(paint));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, arcWidth, arcHeight));
    }

    @Override
    public void drawRoundRect(double x, double y, double w, double h,
                              double arcWidth, double arcHeight, Stroke stroke) {
        applyStroke(stroke);
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, arcWidth, arcHeight));
    }

    @Override
    public void drawLine(double x1, double y1, double x2, double y2, Stroke stroke) {
        applyStroke(stroke);
        g2.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    @Override
    public void fill(Shape shape, Paint paint) {
        g2.setPaint(toAwtPaint(paint));
        g2.fill(toAwtShape(shape));
    }

    @Override
    public void draw(Shape shape, Stroke stroke) {
        applyStroke(stroke);
        g2.draw(toAwtShape(shape));
    }

    // -- Image ---------------------------------------------------------------

    @Override
    public void drawImage(ImageHandle image, double x, double y) {
        AwtImageHandle handle = unwrap(image);
        AffineTransform tx = AffineTransform.getTranslateInstance(x, y);
        g2.drawImage(handle.image(), tx, null);
    }

    @Override
    public void drawImage(ImageHandle image, Region region) {
        AwtImageHandle handle = unwrap(image);
        g2.drawImage(handle.image(),
                region.getX(), region.getY(),
                region.getWidth(), region.getHeight(),
                null);
    }

    // -- Text ----------------------------------------------------------------

    @Override
    public void drawString(String text, double x, double y, Font font, Paint paint) {
        g2.setFont(toAwtFont(font));
        g2.setPaint(toAwtPaint(paint));
        g2.drawString(text, (float) x, (float) y);
    }

    @Override
    public FontMetrics measureText(String text, Font font) {
        java.awt.FontMetrics fm = g2.getFontMetrics(toAwtFont(font));
        double width = fm.stringWidth(text);
        return new AwtFontMetrics(
                width,
                fm.getHeight(),
                fm.getAscent(),
                fm.getDescent(),
                fm.getLeading());
    }

    // -- Lambda-scoped global state ------------------------------------------

    @Override
    public void withTransform(Transform transform, Consumer<Graphics> block) {
        AffineTransform saved = g2.getTransform();
        try {
            g2.transform(toAwtTransform(transform));
            block.accept(this);
        } finally {
            g2.setTransform(saved);
        }
    }

    @Override
    public void withClip(Shape clip, Consumer<Graphics> block) {
        java.awt.Shape saved = g2.getClip();
        try {
            g2.clip(toAwtShape(clip));
            block.accept(this);
        } finally {
            g2.setClip(saved);
        }
    }

    @Override
    public void withAlpha(double alpha, Consumer<Graphics> block) {
        Composite saved = g2.getComposite();
        try {
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, (float) clampAlpha(alpha)));
            block.accept(this);
        } finally {
            g2.setComposite(saved);
        }
    }

    @Override
    public void withBlendMode(BlendMode mode, Consumer<Graphics> block) {
        Composite saved = g2.getComposite();
        try {
            g2.setComposite(toAwtComposite(mode));
            block.accept(this);
        } finally {
            g2.setComposite(saved);
        }
    }

    // -- Converters ----------------------------------------------------------

    private static java.awt.Paint toAwtPaint(Paint paint) {
        return switch (paint) {
            case Paint.Color c -> Settings.color(c.getHex());
        };
    }

    private void applyStroke(Stroke stroke) {
        switch (stroke) {
            case Stroke.Solid s -> {
                g2.setStroke(new BasicStroke((float) s.getWidth()));
                g2.setPaint(toAwtPaint(s.getPaint()));
            }
        }
    }

    private static java.awt.Font toAwtFont(Font font) {
        int style = switch (font.getStyle()) {
            case REGULAR -> java.awt.Font.PLAIN;
            case BOLD -> java.awt.Font.BOLD;
            case ITALIC -> java.awt.Font.ITALIC;
            case BOLD_ITALIC -> java.awt.Font.BOLD | java.awt.Font.ITALIC;
        };
        return new java.awt.Font(font.getFamily(), style, (int) Math.round(font.getSize()));
    }

    private static AffineTransform toAwtTransform(Transform transform) {
        return switch (transform) {
            case Transform.Affine a -> new AffineTransform(
                    a.getScaleX(), a.getShearY(),       // first column  (m00, m10)
                    a.getShearX(), a.getScaleY(),       // second column (m01, m11)
                    a.getTranslateX(), a.getTranslateY());  // third column (m02, m12)
        };
    }

    private static java.awt.Shape toAwtShape(Shape shape) {
        if (shape instanceof AwtShape s) {
            return s.shape();
        }
        throw new IllegalArgumentException(
                "unsupported Shape implementation: " + shape.getClass().getName());
    }

    private static AwtImageHandle unwrap(ImageHandle image) {
        if (image instanceof AwtImageHandle h) {
            return h;
        }
        throw new IllegalArgumentException(
                "unsupported ImageHandle implementation: " + image.getClass().getName());
    }

    private static Composite toAwtComposite(BlendMode mode) {
        int rule = switch (mode) {
            case SRC_OVER -> AlphaComposite.SRC_OVER;
            case SRC      -> AlphaComposite.SRC;
            case DST      -> AlphaComposite.DST;
            case SRC_IN   -> AlphaComposite.SRC_IN;
            case DST_IN   -> AlphaComposite.DST_IN;
            case SRC_OUT  -> AlphaComposite.SRC_OUT;
            case DST_OUT  -> AlphaComposite.DST_OUT;
            case SRC_ATOP -> AlphaComposite.SRC_ATOP;
            case DST_ATOP -> AlphaComposite.DST_ATOP;
            case CLEAR    -> AlphaComposite.CLEAR;
            case XOR      -> AlphaComposite.XOR;
        };
        return AlphaComposite.getInstance(rule);
    }

    private static double clampAlpha(double alpha) {
        if (alpha < 0.0) return 0.0;
        if (alpha > 1.0) return 1.0;
        return alpha;
    }
}
