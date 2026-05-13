package ca.cgjennings.apps.arkham.diy.spi;

import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DIYEditorPanelTest {

    @Test
    void addControlBuffersEntries() {
        JTabbedPane tabs = new JTabbedPane();
        DIYEditorPanel panel = new DIYEditorPanel(tabs);

        panel.addControl("Name", new JTextField());
        panel.addControl("Count", new JTextField());

        assertEquals(2, panel.getControlCount());
        assertEquals(0, tabs.getTabCount());  // not attached yet
    }

    @Test
    void addControlRejectsNonComponent() {
        DIYEditorPanel panel = new DIYEditorPanel(new JTabbedPane());
        assertThrows(IllegalArgumentException.class,
                () -> panel.addControl("Bad", "not a component"));
    }

    @Test
    void addControlRejectsNullControl() {
        DIYEditorPanel panel = new DIYEditorPanel(new JTabbedPane());
        assertThrows(IllegalArgumentException.class,
                () -> panel.addControl("Bad", null));
    }

    @Test
    void addControlRejectsNullLabel() {
        DIYEditorPanel panel = new DIYEditorPanel(new JTabbedPane());
        assertThrows(NullPointerException.class,
                () -> panel.addControl(null, new JTextField()));
    }

    @Test
    void attachAddsTabWithGridContents() {
        JTabbedPane tabs = new JTabbedPane();
        DIYEditorPanel panel = new DIYEditorPanel(tabs);
        JTextField nameField = new JTextField();
        JButton clearButton = new JButton();

        panel.addControl("Name", nameField);
        panel.addControl("Action", clearButton);
        panel.attach();

        assertEquals(1, tabs.getTabCount());
        assertEquals(DIYEditorPanel.TAB_TITLE, tabs.getTitleAt(0));

        JPanel grid = (JPanel) tabs.getComponentAt(0);
        assertEquals(Boolean.TRUE, grid.getClientProperty("se-custom-tab"));

        // 2 rows × 2 columns = 4 components: label, control, label, control
        assertEquals(4, grid.getComponentCount());
        assertEquals("Name", ((JLabel) grid.getComponent(0)).getText());
        assertSame(nameField, grid.getComponent(1));
        assertEquals("Action", ((JLabel) grid.getComponent(2)).getText());
        assertSame(clearButton, grid.getComponent(3));
    }

    @Test
    void attachWithoutControlsIsNoOp() {
        JTabbedPane tabs = new JTabbedPane();
        DIYEditorPanel panel = new DIYEditorPanel(tabs);

        panel.attach();
        assertEquals(0, tabs.getTabCount());
    }

    @Test
    void preExistingTabsArePreserved() {
        JTabbedPane tabs = new JTabbedPane();
        JPanel existing = new JPanel();
        tabs.add("Existing", existing);

        DIYEditorPanel panel = new DIYEditorPanel(tabs);
        panel.addControl("Field", new JTextField());
        panel.attach();

        assertEquals(2, tabs.getTabCount());
        assertSame(existing, tabs.getComponentAt(0));
        assertEquals(DIYEditorPanel.TAB_TITLE, tabs.getTitleAt(1));
    }

    @Test
    void customTabPropertyDistinguishesSpiTab() {
        JTabbedPane tabs = new JTabbedPane();
        JPanel plain = new JPanel();
        tabs.add("Plain", plain);

        DIYEditorPanel panel = new DIYEditorPanel(tabs);
        panel.addControl("X", new JTextField());
        panel.attach();

        // Only the SPI-added tab carries the marker property.
        assertNull(plain.getClientProperty("se-custom-tab"));
        JPanel spiTab = (JPanel) tabs.getComponentAt(1);
        assertNotNull(spiTab.getClientProperty("se-custom-tab"));
        assertTrue((Boolean) spiTab.getClientProperty("se-custom-tab"));
    }

    @Test
    void acceptsAnyComponentSubtype() {
        // EditorPanel.addControl is typed as Object; verify the cast
        // accepts any java.awt.Component, not just JComponent.
        DIYEditorPanel panel = new DIYEditorPanel(new JTabbedPane());
        Component awtComp = new java.awt.Label("legacy AWT label");
        panel.addControl("Legacy", awtComp);
        assertEquals(1, panel.getControlCount());
    }
}
