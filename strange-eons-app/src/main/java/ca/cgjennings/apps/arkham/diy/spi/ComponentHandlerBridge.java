package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYEditor;
import ca.cgjennings.apps.arkham.diy.DIYSheet;
import ca.cgjennings.apps.arkham.diy.Handler;
import ca.cgjennings.graphics.spi.AwtGraphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import org.strangeeons.plugins.conventional.ComponentHandler;
import org.strangeeons.plugins.conventional.Face;

/**
 * Bridges a {@link ComponentHandler} (SPI) onto the legacy
 * {@link Handler} interface so a {@link DIY} component built around a
 * SPI handler dispatches through the existing DIY framework without
 * changes to the call sites.
 *
 * <p>At {@code create} time the bridge transfers the handler's
 * declarative configuration ({@code getFaceCount}, {@code
 * getTemplateKey}, {@code getPortraitKey}) onto the DIY's restricted
 * properties. Returning {@code null} for {@code getTemplateKey} or
 * {@code getPortraitKey} leaves the corresponding DIY property unset
 * — the framework's default template-image and portrait pipelines do
 * not run for that face / slot, and the handler is responsible for
 * any custom painting.
 *
 * <p>The bridge holds no DIY reference and no per-component state
 * beyond a per-sheet {@link ConventionalDIYFace} cache keyed by
 * sheet identity. Settings access is provided to the handler via
 * {@link DIY#getSettings()} on the DIY passed in by the framework
 * (and via {@link Face#getSettings()} during paint).
 *
 * <p>Editor wiring: the legacy {@code createInterface(DIY, DIYEditor)}
 * dispatch wraps the editor's tabbed pane in a
 * {@link DIYEditorPanel} and passes it through the SPI. After the
 * handler returns, the panel is attached to the editor as a custom
 * tab containing the collected controls.
 */
public final class ComponentHandlerBridge implements Handler {

    private final ComponentHandler delegate;
    private final Map<DIYSheet, ConventionalDIYFace> faces = new IdentityHashMap<>();

    public ComponentHandlerBridge(ComponentHandler delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /** Returns the wrapped SPI handler. */
    public ComponentHandler getDelegate() {
        return delegate;
    }

    private Face faceFor(DIYSheet sheet) {
        ConventionalDIYFace cached = faces.get(sheet);
        if (cached == null) {
            cached = new ConventionalDIYFace(sheet);
            faces.put(sheet, cached);
        }
        return cached;
    }

    private static DIY.FaceStyle faceStyleFor(int count) {
        return switch (count) {
            case 1 -> DIY.FaceStyle.ONE_FACE;
            case 2 -> DIY.FaceStyle.TWO_FACES;
            case 3 -> DIY.FaceStyle.CARD_AND_MARKER;
            case 4 -> DIY.FaceStyle.FOUR_FACES;
            case 6 -> DIY.FaceStyle.SIX_FACES;
            case 8 -> DIY.FaceStyle.EIGHT_FACES;
            default -> throw new IllegalArgumentException(
                    "no legacy FaceStyle for face count: " + count);
        };
    }

    @Override
    public void create(DIY diy) {
        int count = delegate.getFaceCount();
        diy.setFaceStyle(faceStyleFor(count));
        for (int i = 0; i < count; i++) {
            String key = delegate.getTemplateKey(i);
            if (key != null) {
                diy.setTemplateKey(i, key);
            }
        }
        String portraitKey = delegate.getPortraitKey();
        if (portraitKey != null) {
            diy.setPortraitKey(portraitKey);
        }
        delegate.create(diy.getSettings());
    }

    @Override
    public void createInterface(DIY diy, DIYEditor editor) {
        DIYEditorPanel panel = new DIYEditorPanel(editor.getTabbedPane());
        delegate.createInterface(diy.getSettings(), panel);
        panel.attach();
    }

    @Override
    public void createFrontPainter(DIY diy, DIYSheet sheet) {
        delegate.createPainter(faceFor(sheet));
    }

    @Override
    public void createBackPainter(DIY diy, DIYSheet sheet) {
        delegate.createPainter(faceFor(sheet));
    }

    @Override
    public void paintFront(Graphics2D g, DIY diy, DIYSheet sheet) {
        delegate.paint(faceFor(sheet), new AwtGraphics(g));
    }

    @Override
    public void paintBack(Graphics2D g, DIY diy, DIYSheet sheet) {
        delegate.paint(faceFor(sheet), new AwtGraphics(g));
    }

    @Override
    public void onClear(DIY diy) {
        delegate.onClear(diy.getSettings());
    }

    @Override
    public void onRead(DIY diy, ObjectInputStream in) {
        try {
            delegate.onRead(diy.getSettings(), in);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onWrite(DIY diy, ObjectOutputStream out) {
        try {
            delegate.onWrite(diy.getSettings(), out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int getPortraitCount() {
        return delegate.getPortraitCount();
    }

    @Override
    public Portrait getPortrait(int index) {
        return (Portrait) delegate.getPortrait(index);
    }
}
