package org.strangeeons.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;
import org.strangeeons.core.observable.AtomicObservable;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.ComponentHandler;
import org.strangeeons.plugins.conventional.EditorControls;
import org.strangeeons.plugins.conventional.EditorPanel;

class TestEditorPanelTest {

    /** A two-faced card with a name, a front title, a flag, a type and a side. */
    static final class CardHandler implements ComponentHandler {
        final AtomicInteger populated = new AtomicInteger();

        @Override
        public int getFaceCount() {
            return 2;
        }

        @Override
        public void create(SettingsReadWrite settings) {
            settings.set("title", "Untitled");
        }

        @Override
        public void createInterface(SettingsReadWrite settings, EditorPanel panel) {
            EditorControls c = panel.getControls();
            JTextField name = c.textField("", 20);
            name.setName("name");
            panel.setNameField(name);

            JTextField title = c.textField("", 20);
            title.setName("title");
            panel.bind("title", title, 0);

            JCheckBox flag = new JCheckBox();
            flag.setName("flag");
            panel.bind("flag", flag);

            JComboBox<Object> type = c.comboBox(List.of("ally", "enemy"), v -> v.toUpperCase());
            type.setName("type");
            panel.bind("type", type, 1);

            JRadioButton left = new JRadioButton("Left");
            JRadioButton right = new JRadioButton("Right");
            left.setName("left");
            right.setName("right");
            panel.bindGroup("side", List.of(left, right), List.of("L", "R"));

            panel.bind("note", new AtomicObservable<>("observable"));
            panel.onPopulate(populated::incrementAndGet);

            JPanel main = new JPanel(new GridLayout(0, 1));
            for (Component comp : new Component[]{name, title, flag, type, left, right}) {
                main.add(comp);
            }
            panel.addTab("Main", main);
            JTextArea rules = c.noMarkup(c.textArea("Rules", 4, 20));
            rules.setName("rules");
            panel.addTab("Rules", rules);
        }
    }

    @Test
    void buildsAndPopulatesTheHandlerInterface() {
        TestSettings settings = new TestSettings();
        CardHandler handler = new CardHandler();
        TestEditorPanel panel = TestEditorPanel.build(handler, settings);

        assertEquals(List.of("Main", "Rules"), panel.getTabTitles());
        assertEquals(1, handler.populated.get());
        assertEquals("Untitled", panel.find("title", JTextField.class).getText());
        assertEquals("0", settings.get("flag"));
        assertEquals("ally", settings.get("type"));
        assertNull(settings.get("side"));
        assertEquals("observable", settings.get("note"));
        assertSame(panel.find("name", JTextField.class), panel.getNameField());
        assertEquals(Set.of(), panel.getChangedFaces());
    }

    @Test
    void controlChangesUpdateSettingsAndMarkFaces() {
        TestSettings settings = new TestSettings();
        TestEditorPanel panel = TestEditorPanel.build(new CardHandler(), settings);

        panel.find("title", JTextField.class).setText("Gloin");
        assertEquals("Gloin", settings.get("title"));
        assertEquals(Set.of(0), panel.getChangedFaces());

        panel.clearChanges();
        @SuppressWarnings("unchecked")
        JComboBox<Object> type = panel.find("type", JComboBox.class);
        type.setSelectedIndex(1);
        assertEquals("enemy", settings.get("type"));
        assertEquals(Set.of(1), panel.getChangedFaces());

        panel.clearChanges();
        panel.find("flag", JCheckBox.class).setSelected(true);
        assertEquals("1", settings.get("flag"));
        assertEquals(Set.of(0, 1), panel.getChangedFaces());

        panel.find("right", JRadioButton.class).doClick();
        assertEquals("R", settings.get("side"));

        panel.markChanged(1);
        assertTrue(panel.getChangedFaces().contains(1));
    }

    @Test
    void populateCopiesSettingsIntoControls() {
        TestSettings settings = new TestSettings();
        CardHandler handler = new CardHandler();
        TestEditorPanel panel = TestEditorPanel.build(handler, settings);
        settings.set("title", "Dori");
        settings.set("side", "L");
        settings.setYesNo("flag", true);
        panel.populate();
        assertEquals("Dori", panel.find("title", JTextField.class).getText());
        assertTrue(panel.find("left", JRadioButton.class).isSelected());
        assertTrue(panel.find("flag", JCheckBox.class).isSelected());
        assertEquals(2, handler.populated.get());
        assertEquals(Set.of(), panel.getChangedFaces());
    }

    @Test
    void comboBoxesShowLabelsButStoreValues() {
        TestEditorPanel panel = TestEditorPanel.build(new CardHandler(), new TestSettings());
        @SuppressWarnings("unchecked")
        JComboBox<Object> type = panel.find("type", JComboBox.class);
        Object item = type.getItemAt(1);
        assertEquals("enemy", item.toString());
        Component shown = type.getRenderer().getListCellRendererComponent(new JList<>(), item, 1, false, false);
        assertEquals("ENEMY", ((JLabel) shown).getText());
        Component plain = type.getRenderer().getListCellRendererComponent(new JList<>(), "raw", 0, false, false);
        assertEquals("raw", ((DefaultListCellRenderer) plain).getText());

        JComboBox<Object> editable = panel.getControls().editableComboBox(List.of("x", "y"));
        assertTrue(editable.isEditable());
        assertEquals(2, editable.getItemCount());
    }

    @Test
    void paintsTabs() {
        TestEditorPanel panel = TestEditorPanel.build(new CardHandler(), new TestSettings());
        BufferedImage image = panel.paintTab("Main", 300, 200);
        assertEquals(300, image.getWidth());
        boolean inked = false;
        for (int y = 0; y < image.getHeight() && !inked; ++y) {
            for (int x = 0; x < image.getWidth() && !inked; ++x) {
                inked = (image.getRGB(x, y) >>> 24) != 0;
            }
        }
        assertTrue(inked);
        assertThrows(IllegalArgumentException.class, () -> panel.paintTab("Missing", 10, 10));
    }

    @Test
    void reportsMissingControlsAndBadArguments() {
        TestEditorPanel panel = TestEditorPanel.build(new CardHandler(), new TestSettings());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> panel.find("title", JCheckBox.class));
        assertTrue(e.getMessage().contains("JCheckBox named \"title\""), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> panel.find("nothing", Component.class));
        assertThrows(IllegalArgumentException.class, () -> new TestEditorPanel(new TestSettings(), 0));
        assertThrows(NullPointerException.class, () -> new TestEditorPanel(null, 1));
        assertThrows(NullPointerException.class, () -> TestEditorPanel.build(null, new TestSettings()));
        assertThrows(NullPointerException.class, () -> panel.onPopulate(null));
        assertThrows(NullPointerException.class, () -> panel.getControls().noMarkup(null));
        assertThrows(IllegalArgumentException.class,
                () -> panel.bind("bad", new JTextField(), 2));
        assertSame(panel.getTabs(), panel.getTabs());
    }
}
