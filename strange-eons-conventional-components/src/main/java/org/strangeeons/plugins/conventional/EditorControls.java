package org.strangeeons.plugins.conventional;

import java.util.List;
import java.util.function.Function;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Factories for Swing controls that integrate with the application:
 * the editor font, spell checking, and consistent list rendering.
 * Plain Swing controls can be used directly. Call all methods on the
 * event dispatch thread.
 *
 * @since 3.6
 */
public interface EditorControls {

    /** Creates a spell-checked text field in the editor font. */
    JTextField textField(String text, int columns);

    /**
     * Creates a spell-checked, word-wrapping text area in the editor
     * font. It does not scroll on its own.
     */
    JTextArea textArea(String text, int rows, int columns);

    /**
     * Creates a non-editable combo box whose items' string forms are
     * {@code values}. Labels are looked up each time an item is drawn,
     * so they can depend on the state of the component.
     */
    JComboBox<Object> comboBox(List<String> values, Function<String, String> labels);

    /** Creates a combo box that suggests values and accepts typed text. */
    JComboBox<Object> editableComboBox(List<String> suggestions);

    /**
     * Excludes a control from markup insertion and the context bar, for
     * fields whose text is not markup.
     *
     * @return the control
     */
    <T extends JComponent> T noMarkup(T control);
}
