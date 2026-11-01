package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.ContextBar;
import ca.cgjennings.apps.arkham.MarkupTarget;
import ca.cgjennings.apps.arkham.diy.ListItem;
import ca.cgjennings.apps.arkham.diy.ListItemRenderer;
import ca.cgjennings.spelling.ui.JSpellingTextArea;
import ca.cgjennings.spelling.ui.JSpellingTextField;
import java.util.List;
import java.util.function.Function;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.strangeeons.plugins.conventional.EditorControls;
import resources.ResourceKit;

/** {@link EditorControls} built from the application's Swing controls. */
final class DIYEditorControls implements EditorControls {

    private static final int MAX_VISIBLE_ROWS = 12;

    @Override
    public JTextField textField(String text, int columns) {
        JTextField field = new JSpellingTextField(text, columns);
        field.setFont(ResourceKit.getEditorFont());
        field.select(0, 0);
        return field;
    }

    @Override
    public JTextArea textArea(String text, int rows, int columns) {
        JTextArea area = new JSpellingTextArea(text, rows, columns);
        area.setFont(ResourceKit.getEditorFont());
        area.select(0, 0);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setTabSize(4);
        return area;
    }

    @Override
    public JComboBox<Object> comboBox(List<String> values, Function<String, String> labels) {
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        for (String value : values) {
            model.addElement(new ListItem(value, new Object() {
                @Override
                public String toString() {
                    return labels.apply(value);
                }
            }));
        }
        return configure(new JComboBox<>(model));
    }

    @Override
    public JComboBox<Object> editableComboBox(List<String> suggestions) {
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        suggestions.forEach(model::addElement);
        JComboBox<Object> combo = configure(new JComboBox<>(model));
        combo.setEditable(true);
        noMarkup((JComponent) combo.getEditor().getEditorComponent());
        return combo;
    }

    private static JComboBox<Object> configure(JComboBox<Object> combo) {
        combo.setRenderer(ListItemRenderer.getShared());
        combo.setMaximumRowCount(MAX_VISIBLE_ROWS);
        return combo;
    }

    @Override
    public <T extends JComponent> T noMarkup(T control) {
        control.putClientProperty(MarkupTarget.FORCE_MARKUP_TARGET_PROPERTY, Boolean.FALSE);
        control.putClientProperty(ContextBar.BAR_DISABLE_PROPERTY, Boolean.TRUE);
        return control;
    }
}
