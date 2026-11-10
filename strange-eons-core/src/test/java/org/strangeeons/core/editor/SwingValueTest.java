package org.strangeeons.core.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

class SwingValueTest {

    @Test
    void bindsTextComponents() throws Exception {
        JTextField field = new JTextField("start");
        SwingValue value = SwingValue.of(field);
        List<String> seen = new ArrayList<>();
        AutoCloseable sub = value.subscribe(seen::add);
        assertThat(value.get()).isEqualTo("start");
        value.set("next");
        assertThat(field.getText()).isEqualTo("next");
        assertThat(field.getSelectionStart()).isZero();
        field.setText("typed");
        assertThat(seen).contains("typed");
        sub.close();
        seen.clear();
        field.setText("ignored");
        assertThat(seen).isEmpty();
        value.set("ignored");
        assertThatThrownBy(() -> value.subscribe(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void unwrapsScrollPanes() {
        JTextArea area = new JTextArea("text");
        SwingValue value = SwingValue.of(new JScrollPane(area));
        value.set("changed");
        assertThat(area.getText()).isEqualTo("changed");
    }

    @Test
    void bindsToggles() {
        JCheckBox box = new JCheckBox();
        SwingValue value = SwingValue.of(box);
        assertThat(value.get()).isEqualTo("0");
        value.set("yes");
        assertThat(box.isSelected()).isTrue();
        assertThat(value.get()).isEqualTo("1");
        List<String> seen = new ArrayList<>();
        value.subscribe(seen::add);
        box.setSelected(false);
        assertThat(seen).containsExactly("0");
    }

    @Test
    void bindsComboBoxes() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"a", "b", "c"});
        SwingValue value = SwingValue.of(combo);
        List<String> seen = new ArrayList<>();
        value.subscribe(seen::add);
        value.set("c");
        assertThat(combo.getSelectedIndex()).isEqualTo(2);
        value.set("unknown");
        assertThat(combo.getSelectedIndex()).isEqualTo(2);
        assertThat(seen).containsExactly("c");
    }

    @Test
    void bindsEditableComboBoxes() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"a", "b"});
        combo.setEditable(true);
        SwingValue value = SwingValue.of(combo);
        value.set("free text");
        assertThat(value.get()).isEqualTo("free text");
    }

    @Test
    void bindsButtonGroups() {
        JRadioButton left = new JRadioButton();
        JRadioButton right = new JRadioButton();
        SwingValue value = SwingValue.ofGroup(List.of(left, right), List.of("L", "R"));
        assertThat(value.get()).isNull();
        value.set("R");
        assertThat(right.isSelected()).isTrue();
        value.set("L");
        assertThat(left.isSelected()).isTrue();
        assertThat(right.isSelected()).isFalse();
        value.set("unknown");
        assertThat(value.get()).isEqualTo("L");
        List<String> seen = new ArrayList<>();
        value.subscribe(seen::add);
        right.doClick();
        assertThat(seen).containsExactly("R");
    }

    @Test
    void rejectsBadGroupsAndControls() {
        JRadioButton b = new JRadioButton();
        assertThatThrownBy(() -> SwingValue.ofGroup(List.of(), List.of())).hasMessageContaining("no buttons");
        assertThatThrownBy(() -> SwingValue.ofGroup(List.of(b), List.of("a", "b"))).hasMessageContaining("number of values");
        assertThatThrownBy(() -> SwingValue.of(new JLabel())).hasMessageContaining("no standard binding");
        assertThatThrownBy(() -> SwingValue.of(null)).isInstanceOf(NullPointerException.class);
    }
}
