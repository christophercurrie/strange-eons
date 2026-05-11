package org.strangeeons.plugins.conventional;

/**
 * The container that a {@link ComponentHandler}'s
 * {@code createInterface} hook populates with the component's editor
 * controls. Plug-ins call {@link #addControl(String, Object)} once
 * per control they want to surface in the editor; the framework
 * arranges layout, labels, and any binding wiring.
 *
 * <p>Controls are currently supplied as opaque {@code Object}
 * references; the specific control vocabulary is intentionally left
 * open. Concrete control types are recognized by the active UI
 * toolkit binding.
 *
 * @since 3.6
 */
public interface EditorPanel {

    /**
     * Adds a labeled control to the editor. The {@code control}
     * argument is whatever control object the active UI toolkit
     * recognizes; the framework attaches it to the layout and wires
     * up any settings binding the control declares.
     */
    void addControl(String label, Object control);
}
