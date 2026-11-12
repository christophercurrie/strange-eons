package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.image.BufferedImage;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TabbedPaneNavigatorTest {
    private JTabbedPane tabs;
    private TabbedPaneNavigator navigator;
    private JList<?> list;

    private void create() {
        tabs = new JTabbedPane();
        tabs.addTab("General", new JPanel());
        tabs.addTab("Game Text: Front", new JPanel());
        tabs.addTab("Comments", new JPanel());
        tabs.setSelectedIndex(1);
        navigator = new TabbedPaneNavigator(tabs);
        list = (JList<?>) ((JScrollPane) navigator.getLeftComponent()).getViewport().getView();
    }

    @Test void selectionWorksInBothDirections() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            create();

            assertThat(list.getSelectedIndex()).isEqualTo(1);

            list.setSelectedIndex(2);

            assertThat(tabs.getSelectedIndex()).isEqualTo(2);

            tabs.setSelectedIndex(0);

            assertThat(list.getSelectedIndex()).isEqualTo(0);
            assertThat(tabs.getComponentAt(0).isVisible()).isTrue();
            assertThat(tabs.getComponentAt(2).isVisible()).isFalse();

            list.clearSelection();

            assertThat(list.getSelectedIndex()).isEqualTo(0);

            list.getSelectionModel().setValueIsAdjusting(true);
            list.setSelectedIndex(1);

            assertThat(tabs.getSelectedIndex()).isEqualTo(0);

            list.getSelectionModel().setValueIsAdjusting(false);

            assertThat(tabs.getSelectedIndex()).isEqualTo(1);
        });
    }

    @Test void followsDynamicPagesAndTitles() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            create();
            tabs.insertTab("Portraits", null, new JPanel(), null, 0);

            assertThat(list.getModel().getSize()).isEqualTo(4);
            assertThat(list.getModel().getElementAt(0)).isEqualTo("Portraits");

            tabs.setTitleAt(1, "Renamed");

            assertThat(list.getModel().getElementAt(1)).isEqualTo("Renamed");

            tabs.removeTabAt(tabs.getSelectedIndex());

            assertThat(list.getSelectedIndex()).isEqualTo(tabs.getSelectedIndex());

            tabs.removeAll();

            assertThat(list.getModel().getSize()).isEqualTo(0);
            assertThat(list.getSelectedIndex()).isEqualTo(-1);

            tabs.addTab("New", new JPanel());

            assertThat(list.getSelectedIndex()).isEqualTo(0);
        });
    }

    @Test void disabledPagesCannotBeSelected() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            create();
            tabs.setEnabledAt(2, false);
            list.setSelectedIndex(2);

            assertThat(tabs.getSelectedIndex()).isEqualTo(1);
            assertThat(list.getSelectedIndex()).isEqualTo(1);

            @SuppressWarnings("unchecked")
            JList<String> labels = (JList<String>) list;
            Component cell = labels.getCellRenderer().getListCellRendererComponent(labels, "Comments", 2, false, false);

            assertThat(cell.isEnabled()).isFalse();

            cell = labels.getCellRenderer().getListCellRendererComponent(labels, "General", 0, true, true);

            assertThat(cell.isEnabled()).isTrue();
            assertThat(((JLabel) cell).getText()).isEqualTo("General");
            assertThat(((JLabel) cell).getIcon()).isNull();
            assertThat(labels.getCellRenderer().getListCellRendererComponent(labels, "", -1, false, false).isEnabled()).isTrue();
        });
    }

    @Test void hidesTabStripAfterLookAndFeelUpdates() throws Exception {
        SwingUtilities.invokeAndWait(this::create);
        for (int placement : new int[]{JTabbedPane.TOP, JTabbedPane.LEFT}) {
            SwingUtilities.invokeAndWait(() -> {
                tabs.setTabPlacement(placement);
                tabs.updateUI();
                tabs.updateUI();
            });
            SwingUtilities.invokeAndWait(() -> {
                tabs.setSize(400, 300);
                tabs.doLayout();

                assertThat(tabs.getSelectedComponent().getY()).isEqualTo(0);
                assertThat(tabs.getSelectedComponent().getX()).isEqualTo(0);

                var image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                try { tabs.paint(graphics); } finally { graphics.dispose(); }

                assertThat(navigator.getRightComponent()).isSameAs(tabs);
            });
        }
    }

    @Test void supportsInitiallyEmptyPane() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            tabs = new JTabbedPane();
            navigator = new TabbedPaneNavigator(tabs);
            tabs.addTab("First", new JPanel());

            assertThat(tabs.getSelectedIndex()).isEqualTo(0);
        });
    }
}
