package org.strangeeons.testkit;

import java.awt.Component;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.strangeeons.plugins.conventional.EditorControls;

/**
 * Editor controls made from standard Swing components. They behave like the
 * application's controls for binding purposes, but have no spelling checker or
 * markup support. A combo box item's {@code toString()} is its setting value;
 * its label is only displayed.
 */
public final class PlainEditorControls implements EditorControls {

    private static final int MAX_VISIBLE_ROWS = 12;

    /** Creates the controls factory. */
    public PlainEditorControls() {
    }

    @Override
    public JTextField textField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        field.select(0, 0);
        return field;
    }

    @Override
    public JTextArea textArea(String text, int rows, int columns) {
        JTextArea area = new JTextArea(text, rows, columns);
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
            model.addElement(new Choice(value, labels.apply(value)));
        }
        JComboBox<Object> combo = new JComboBox<>(model);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Object shown = value instanceof Choice ? ((Choice) value).label : value;
                return super.getListCellRendererComponent(list, shown, index, isSelected, cellHasFocus);
            }
        });
        combo.setMaximumRowCount(MAX_VISIBLE_ROWS);
        return combo;
    }

    @Override
    public JComboBox<Object> editableComboBox(List<String> suggestions) {
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        suggestions.forEach(model::addElement);
        JComboBox<Object> combo = new JComboBox<>(model);
        combo.setMaximumRowCount(MAX_VISIBLE_ROWS);
        combo.setEditable(true);
        return combo;
    }

    @Override
    public <T extends JComponent> T noMarkup(T control) {
        return Objects.requireNonNull(control, "control");
    }

    /** A combo box item: displays its label, converts to its setting value. */
    private static final class Choice {

        private final String value;
        private final String label;

        Choice(String value, String label) {
            this.value = Objects.requireNonNull(value, "value");
            this.label = label == null ? value : label;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
