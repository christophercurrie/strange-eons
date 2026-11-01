package org.strangeeons.plugins.conventional;

import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import org.strangeeons.plugins.observable.MutableObservable;

/**
 * The component editor as seen by a {@link ComponentHandler}'s
 * {@code createInterface} hook. Plug-ins build their controls and
 * layout with ordinary Swing, add the result as a tab, and bind
 * controls to settings. Call all methods on the event dispatch thread.
 *
 * <p>A binding copies the setting to its control when bound and each
 * time the editor is populated, and copies the control to the setting
 * as soon as it changes, marking the listed faces for repainting. If
 * the setting has no value yet, the control's value becomes the
 * setting's default. Omitting {@code faces} repaints every face.
 *
 * @since 3.6
 */
public interface EditorPanel {

    /**
     * Adds a tab to the editor, ahead of the last standard tab. The
     * content is used as supplied; wrap it first if it should scroll.
     */
    void addTab(String title, JComponent content);

    /**
     * Binds a text component, toggle button, or combo box to a setting.
     * A scroll pane binds its view. Toggle buttons store {@code "1"} or
     * {@code "0"}. A non-editable combo box stores the string form of
     * the selected item; an editable one stores the text being edited.
     *
     * @throws IllegalArgumentException if the control has no standard
     *     binding or a face does not exist
     */
    void bind(String key, JComponent control, int... faces);

    /**
     * Binds a custom value to a setting. Use this for controls that have
     * no standard binding, adapting the control to the observable.
     *
     * @throws IllegalArgumentException if a face does not exist
     */
    void bind(String key, MutableObservable<String> value, int... faces);

    /**
     * Groups buttons so that one is selected at a time, and binds the
     * selection to a setting. Each button stores the value at its index.
     *
     * @throws IllegalArgumentException if the lists are empty or differ
     *     in size, or a face does not exist
     */
    void bindGroup(String key, List<? extends AbstractButton> buttons,
            List<String> values, int... faces);

    /**
     * Sets the field that edits the component's name. The field is kept
     * in step with the component's name rather than a setting.
     */
    void setNameField(JTextComponent field);

    /**
     * Registers an action to run each time the editor is populated from
     * the component, after bindings have updated their controls.
     */
    void onPopulate(Runnable action);

    /** Marks a face for repainting and the component as unsaved. */
    void markChanged(int face);

    /** Returns factories for controls that integrate with the application. */
    EditorControls getControls();
}
