package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.ContextBar;
import ca.cgjennings.apps.arkham.MarkupTarget;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYEditor;
import ca.cgjennings.apps.arkham.diy.ListItem;
import org.strangeeons.core.observable.AtomicObservable;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.awt.Canvas;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.ComboBoxEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import resources.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DIYEditorPanelTest {

    private DIY diy;
    private DIYEditor editor;
    private JTabbedPane tabs;
    private Settings settings;
    private DIYEditorPanel panel;

    @BeforeEach
    void setUp() {
        diy = mock(DIY.class);
        editor = mock(DIYEditor.class);
        tabs = new JTabbedPane();
        settings = new Settings();
        when(diy.getSettings()).thenReturn(settings);
        when(editor.getTabbedPane()).thenReturn(tabs);
        panel = new DIYEditorPanel(diy, editor);
    }

    private void withFaces(int count) {
        when(diy.getSheets()).thenReturn(new Sheet<?>[count]);
    }

    private void firePopulate() {
        panel.attach();
        ArgumentCaptor<StrangeEonsEditor.FieldPopulationListener> listener
                = ArgumentCaptor.forClass(StrangeEonsEditor.FieldPopulationListener.class);
        verify(editor).addFieldPopulationListener(listener.capture());
        listener.getValue().fieldsPopulated(editor);
    }

    private static List<AbstractButton> buttons() {
        List<AbstractButton> buttons = new ArrayList<>();
        buttons.add(new JRadioButton());
        buttons.add(new JRadioButton());
        return buttons;
    }

    @Test
    void addTabGoesAheadOfLastStandardTab() {
        JPanel portrait = new JPanel();
        JPanel comments = new JPanel();
        tabs.add("Portrait", portrait);
        tabs.add("Comments", comments);
        JPanel content = new JPanel();

        panel.addTab("Details", content);

        assertThat(tabs.getComponentAt(0)).isSameAs(portrait);
        assertThat(tabs.getComponentAt(1)).isSameAs(content);
        assertThat(tabs.getComponentAt(2)).isSameAs(comments);
        assertThat(tabs.getTitleAt(1)).isEqualTo("Details");
        assertThat(content.getClientProperty(DIYEditorPanel.CUSTOM_TAB_PROPERTY)).isEqualTo("Details");
    }

    @Test
    void addTabToEmptyPane() {
        panel.addTab("Details", new JPanel());
        assertThat(tabs.getTabCount()).isEqualTo(1);
    }

    @Test
    void addTabResetsDividerBeforeEditorShows() {
        JSplitPane splitter = spy(new JSplitPane());
        splitter.setLeftComponent(tabs);

        panel.addTab("Details", new JPanel());

        verify(splitter).setDividerLocation(-1);
    }

    @Test
    void addTabKeepsDividerOnceEditorShows() {
        JSplitPane splitter = spy(new JSplitPane());
        splitter.setLeftComponent(tabs);
        when(editor.isShowing()).thenReturn(true);

        panel.addTab("Details", new JPanel());

        verify(splitter, never()).setDividerLocation(-1);
    }

    @Test
    void unsetSettingTakesControlValueWithoutMarking() {
        panel.bind("Name", new JTextField("Luke"), 0);

        assertThat(settings.get("Name")).isEqualTo("Luke");
        verify(diy, never()).markChanged(anyInt());
    }

    @Test
    void existingSettingFillsControlWithoutMarking() {
        settings.set("Name", "Leia");
        JTextField field = new JTextField("Luke");

        panel.bind("Name", field, 0);

        assertThat(field.getText()).isEqualTo("Leia");
        assertThat(settings.get("Name")).isEqualTo("Leia");
        verify(diy, never()).markChanged(anyInt());
    }

    @Test
    void textChangesPushToSetting() {
        JTextField field = new JTextField();
        panel.bind("Name", field, 0);

        field.setText("Han");

        assertThat(settings.get("Name")).isEqualTo("Han");
        verify(diy, atLeastOnce()).markChanged(0);
    }

    @Test
    void clearingTextPushesEmptyValue() {
        JTextField field = new JTextField("Han");
        panel.bind("Name", field, 0);

        field.setText("");

        assertThat(settings.get("Name")).isEmpty();
    }

    @Test
    void styleChangeLeavesSettingAlone() {
        JTextPane pane = new JTextPane();
        pane.setText("Han");
        panel.bind("Name", pane, 0);

        pane.selectAll();
        pane.setCharacterAttributes(new SimpleAttributeSet(), true);

        assertThat(settings.get("Name")).isEqualTo("Han");
        verify(diy, never()).markChanged(anyInt());
    }

    @Test
    void validFaceIsAcceptedWhenSheetsExist() {
        withFaces(2);
        JTextField field = new JTextField();
        panel.bind("Name", field, 1);

        field.setText("Han");

        verify(diy, atLeastOnce()).markChanged(1);
    }

    @Test
    void scrollPaneWithNonSwingViewHasNoBinding() {
        JScrollPane scroll = new JScrollPane(new Canvas());
        assertThatIllegalArgumentException().isThrownBy(() -> panel.bind("Rules", scroll, 0));
    }

    @Test
    void scrollPaneBindsItsView() {
        JTextArea area = new JTextArea();
        panel.bind("Rules", new JScrollPane(area), 0);

        area.setText("Deploy");

        assertThat(settings.get("Rules")).isEqualTo("Deploy");
    }

    @Test
    void emptyScrollPaneHasNoBinding() {
        assertThatIllegalArgumentException().isThrownBy(() -> panel.bind("Rules", new JScrollPane(), 0));
    }

    @Test
    void populateRefreshesControlWithoutMarking() {
        JTextField field = new JTextField();
        panel.bind("Name", field, 0);
        settings.set("Name", "Chewie");

        firePopulate();

        assertThat(field.getText()).isEqualTo("Chewie");
        verify(diy, never()).markChanged(anyInt());
    }

    @Test
    void populateLeavesUnchangedControlAlone() {
        settings.set("Name", "Lando");
        JTextField field = new JTextField();
        panel.bind("Name", field, 0);
        field.setCaretPosition(3);

        firePopulate();

        assertThat(field.getCaretPosition()).isEqualTo(3);
    }

    @Test
    void toggleStoresYesNoInBothDirections() {
        settings.set("Unique", "1");
        JCheckBox box = new JCheckBox();
        panel.bind("Unique", box, 0);
        assertThat(box.isSelected()).isTrue();

        box.setSelected(false);
        assertThat(settings.get("Unique")).isEqualTo("0");
        verify(diy).markChanged(0);

        box.setSelected(true);
        assertThat(settings.get("Unique")).isEqualTo("1");
    }

    @Test
    void comboSelectsMatchingItemAndStoresChanges() {
        settings.set("Type", "b");
        JComboBox<String> combo = new JComboBox<>(new String[]{"a", "b"});
        panel.bind("Type", combo, 0);
        assertThat(combo.getSelectedItem()).isEqualTo("b");

        combo.setSelectedItem("a");

        assertThat(settings.get("Type")).isEqualTo("a");
    }

    @Test
    void comboIgnoresUnknownSetting() {
        settings.set("Type", "gone");
        JComboBox<String> combo = new JComboBox<>(new String[]{"a", "b"});

        panel.bind("Type", combo, 0);

        assertThat(combo.getSelectedItem()).isEqualTo("a");
        assertThat(settings.get("Type")).isEqualTo("gone");
    }

    @Test
    void emptyComboLeavesSettingUnset() {
        panel.bind("Type", new JComboBox<String>(), 0);
        assertThat(settings.get("Type")).isNull();
    }

    @Test
    void editableComboPushesTextAsItChanges() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"Hyperspeed"});
        combo.setEditable(true);
        settings.set("Label", "Landspeed");
        panel.bind("Label", combo, 0);
        JTextField field = (JTextField) combo.getEditor().getEditorComponent();
        assertThat(field.getText()).isEqualTo("Landspeed");

        field.setText("Maneuver");

        assertThat(settings.get("Label")).isEqualTo("Maneuver");
    }

    @Test
    void editableComboWithoutTextEditorIsRejected() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setEditable(true);
        ComboBoxEditor plain = mock(ComboBoxEditor.class);
        when(plain.getEditorComponent()).thenReturn(new JLabel());
        combo.setEditor(plain);

        assertThatIllegalArgumentException().isThrownBy(() -> panel.bind("Label", combo, 0));
    }

    @Test
    void groupSelectsFromSettingAndStoresClicks() {
        settings.set("Side", "dark");
        JRadioButton light = new JRadioButton();
        JRadioButton dark = new JRadioButton();
        panel.bindGroup("Side", List.of(light, dark), List.of("light", "dark"), 0, 1);
        assertThat(dark.isSelected()).isTrue();

        light.doClick();

        assertThat(light.isSelected()).isTrue();
        assertThat(dark.isSelected()).isFalse();
        assertThat(settings.get("Side")).isEqualTo("light");
        verify(diy).markChanged(0);
        verify(diy).markChanged(1);
    }

    @Test
    void groupWithoutSelectionLeavesSettingUnset() {
        panel.bindGroup("Side", buttons(), List.of("light", "dark"), 0);
        assertThat(settings.get("Side")).isNull();
    }

    @Test
    void groupDefaultComesFromSelectedButton() {
        List<AbstractButton> buttons = buttons();
        buttons.get(1).setSelected(true);

        panel.bindGroup("Side", buttons, List.of("light", "dark"), 0);

        assertThat(settings.get("Side")).isEqualTo("dark");
    }

    @Test
    void groupIgnoresUnknownSetting() {
        settings.set("Side", "grey");
        List<AbstractButton> buttons = buttons();
        buttons.get(0).setSelected(true);

        panel.bindGroup("Side", buttons, List.of("light", "dark"), 0);

        assertThat(buttons.get(0).isSelected()).isTrue();
    }

    @Test
    void groupRejectsMismatchedValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> panel.bindGroup("Side", buttons(), List.of("light"), 0));
    }

    @Test
    void customObservableBinds() {
        AtomicObservable<String> value = new AtomicObservable<>("seed");
        panel.bind("Custom", value, 0);
        assertThat(settings.get("Custom")).isEqualTo("seed");

        value.set("next");

        assertThat(settings.get("Custom")).isEqualTo("next");
        verify(diy).markChanged(0);
    }

    @Test
    void nullValueIsNotStored() {
        AtomicObservable<String> value = new AtomicObservable<>("seed");
        panel.bind("Custom", value, 0);

        value.set(null);

        assertThat(settings.get("Custom")).isEqualTo("seed");
    }

    @Test
    void valueEqualToSettingDoesNotMark() {
        AtomicObservable<String> value = new AtomicObservable<>("seed");
        panel.bind("Custom", value, 0);
        settings.set("Custom", "next");

        value.set("next");

        verify(diy, never()).markChanged(anyInt());
    }

    @Test
    void unsupportedControlIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> panel.bind("Bad", new JLabel(), 0));
    }

    @Test
    void negativeFaceIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> panel.bind("Name", new JTextField(), -1));
    }

    @Test
    void faceBeyondSheetsIsRejected() {
        withFaces(2);
        assertThatIllegalArgumentException().isThrownBy(() -> panel.bind("Name", new JTextField(), 2));
    }

    @Test
    void noFacesMarksEverySheet() {
        withFaces(2);
        AtomicObservable<String> value = new AtomicObservable<>("seed");
        panel.bind("Custom", value);

        value.set("next");

        verify(diy).markChanged(0);
        verify(diy).markChanged(1);
    }

    @Test
    void noFacesWithoutSheetsStillMarksUnsaved() {
        AtomicObservable<String> value = new AtomicObservable<>("seed");
        panel.bind("Custom", value);

        value.set("next");

        verify(diy).markChanged(0);
    }

    @Test
    void populateActionsRunAfterBindings() {
        JTextField field = new JTextField();
        panel.bind("Name", field, 0);
        settings.set("Name", "Yoda");
        List<String> seen = new ArrayList<>();
        panel.onPopulate(() -> seen.add(field.getText()));

        firePopulate();

        assertThat(seen).isEqualTo(List.of("Yoda"));
    }

    @Test
    void nameFieldAndMarkChangedForwardToComponent() {
        JTextField field = new JTextField();
        panel.setNameField(field);
        panel.markChanged(1);

        verify(diy).setNameField(field);
        verify(diy).markChanged(1);
    }

    @Test
    void textControlsAreConfigured() {
        JTextField field = panel.getControls().textField("x", 7);
        assertThat(field.getText()).isEqualTo("x");
        assertThat(field.getColumns()).isEqualTo(7);

        JTextArea area = panel.getControls().textArea("y", 3, 9);
        assertThat(area.getRows()).isEqualTo(3);
        assertThat(area.getLineWrap()).isTrue();
        assertThat(area.getWrapStyleWord()).isTrue();
    }

    @Test
    void comboStoresValuesAndLooksUpLabelsWhenDrawn() {
        String[] suffix = {"light"};
        JComboBox<Object> combo = panel.getControls().comboBox(
                List.of("site", "system"), v -> v + "-" + suffix[0]);
        settings.set("Type", "system");
        panel.bind("Type", combo, 0);

        ListItem item = (ListItem) combo.getSelectedItem();
        assertThat(item.toString()).isEqualTo("system");
        assertThat(item.getLabel()).isEqualTo("system-light");
        suffix[0] = "dark";
        assertThat(item.getLabel()).isEqualTo("system-dark");

        combo.setSelectedIndex(0);
        assertThat(settings.get("Type")).isEqualTo("site");
    }

    @Test
    void editableComboSuggestsAndExcludesMarkup() {
        JComboBox<Object> combo = panel.getControls().editableComboBox(List.of("a", "b"));
        assertThat(combo.isEditable()).isTrue();
        assertThat(combo.getItemCount()).isEqualTo(2);
        JTextField field = (JTextField) combo.getEditor().getEditorComponent();
        assertThat(field.getClientProperty(MarkupTarget.FORCE_MARKUP_TARGET_PROPERTY)).isEqualTo(Boolean.FALSE);
        assertThat(field.getClientProperty(ContextBar.BAR_DISABLE_PROPERTY)).isEqualTo(Boolean.TRUE);
    }
}
