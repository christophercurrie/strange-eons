package org.strangeeons.plugins.conventional;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.component.Portrait;
import org.strangeeons.plugins.graphics.Graphics;

/**
 * The behavioral contract a plug-in implements to participate in the
 * Conventional component framework. The handler is both the
 * declarative description of the component (face count, template
 * keys, portrait key) and its painter/lifecycle logic.
 *
 * <p>Declarative getters describe the component's structure. The
 * framework reads them once at construction time, before {@link
 * #create}, and configures the underlying component accordingly.
 * Returning {@code null} from {@link #getTemplateKey} or
 * {@link #getPortraitKey} signals that the handler will paint the
 * template image and / or handle portraits itself, without the
 * framework's default machinery.
 *
 * <p>Lifecycle methods receive a {@link SettingsReadWrite} for the
 * component's settings scope when settings access is meaningful, and
 * a {@link Face} when per-face data is in scope. The handler holds
 * no framework-managed state; whatever state it needs across calls
 * lives in its own fields.
 *
 * @since 3.6
 */
public interface ComponentHandler {

    // ---- Declarative configuration --------------------------------

    /**
     * Returns the number of distinct faces this component draws.
     * Default is {@code 1}; override for multi-face components.
     */
    default int getFaceCount() {
        return 1;
    }

    /**
     * Returns the settings key prefix for the face at
     * {@code faceIndex}, or {@code null} to opt out of the
     * framework's default template-image painting for that face (the
     * handler paints the template itself in {@link #paint}).
     */
    default String getTemplateKey(int faceIndex) {
        return null;
    }

    /**
     * Returns the portrait key prefix used to resolve the framework's
     * default portrait pipeline (clip region, default image, scale,
     * pan), or {@code null} to opt out (the handler handles portraits
     * itself, or the component has no portrait).
     */
    default String getPortraitKey() {
        return null;
    }

    // ---- Lifecycle ------------------------------------------------

    /**
     * Called once when the component is first created. Default is a
     * no-op; override to seed default values in {@code settings}.
     */
    default void create(SettingsReadWrite settings) {
    }

    /**
     * Called when the component's editor opens. Default is a no-op;
     * override to add controls to {@code panel}.
     */
    default void createInterface(SettingsReadWrite settings, EditorPanel panel) {
    }

    /**
     * Called once per face when the framework needs to allocate or
     * refresh painter state.
     */
    default void createPainter(Face face) {
    }

    /**
     * Paints {@code face} into {@code g}. Default is a no-op;
     * override and branch on {@link Face#getFaceIndex()} for
     * per-face logic. A handler that overrides nothing draws an
     * empty face.
     */
    default void paint(Face face, Graphics g) {
    }

    /**
     * Called when the user resets the component to a blank state.
     * Default is a no-op.
     */
    default void onClear(SettingsReadWrite settings) {
    }

    /**
     * Reads handler-specific state from the component's saved form.
     * The stream contains only bytes the handler wrote via
     * {@link #onWrite}; the framework manages container metadata
     * separately. Default is a no-op (handler has no persisted state).
     */
    default void onRead(SettingsReadWrite settings, InputStream in) throws IOException {
    }

    /**
     * Writes handler-specific state to the component's saved form.
     * Default is a no-op (handler has no persisted state).
     */
    default void onWrite(SettingsReadWrite settings, OutputStream out) throws IOException {
    }

    /**
     * Migration hook for plug-ins replacing a legacy handler whose
     * saved files used Java object serialization. The default
     * implementation throws so undeclared migrations fail loudly.
     */
    default void readLegacy(SettingsReadWrite settings, ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        throw new IOException("plug-in does not support legacy save format");
    }

    // ---- Portraits (custom-handling path) -------------------------

    /**
     * Returns the number of portrait slots when the handler opts out
     * of the framework's default portrait pipeline (i.e. when
     * {@link #getPortraitKey()} returns {@code null} but the handler
     * still exposes portraits). Default returns {@code 0}.
     */
    default int getPortraitCount() {
        return 0;
    }

    /**
     * Returns the portrait at the given index. Default returns
     * {@code null}; override when {@link #getPortraitCount()} is
     * non-zero.
     */
    default Portrait getPortrait(int index) {
        return null;
    }
}
