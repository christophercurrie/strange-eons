package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/** Displays a tabbed pane's pages using a plain side navigation list. */
public final class TabbedPaneNavigator extends JSplitPane {
    private final JTabbedPane tabs;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private boolean updating;

    public TabbedPaneNavigator(JTabbedPane tabs) {
        super(HORIZONTAL_SPLIT);
        this.tabs = tabs;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> source, Object value,
                    int index, boolean selected, boolean focused) {
                Component cell = super.getListCellRendererComponent(source, value, index, selected, focused);
                cell.setEnabled(index < 0 || tabs.isEnabledAt(index));
                return cell;
            }
        });
        list.addListSelectionListener(e -> {
            if (!updating && !e.getValueIsAdjusting()) {
                int index = list.getSelectedIndex();
                if (index >= 0 && tabs.isEnabledAt(index)) {
                    tabs.setSelectedIndex(index);
                } else {
                    synchronize();
                }
            }
        });
        tabs.addChangeListener(e -> synchronize());
        tabs.addContainerListener(new ContainerAdapter() {
            @Override public void componentAdded(ContainerEvent e) { synchronize(); }
            @Override public void componentRemoved(ContainerEvent e) { synchronize(); }
        });
        tabs.addPropertyChangeListener(e -> {
            if ("UI".equals(e.getPropertyName()) && !(tabs.getUI() instanceof PageUI)) {
                // Wait until the previous UI has finished notifying its listeners.
                SwingUtilities.invokeLater(() -> {
                    if (!(tabs.getUI() instanceof PageUI)) {
                        tabs.setUI(new PageUI());
                    }
                });
            }
            synchronize();
        });
        tabs.setUI(new PageUI());
        JScrollPane scroll = new JScrollPane(list);
        scroll.setMinimumSize(new Dimension(80, 0));
        scroll.setPreferredSize(new Dimension(Math.min(240,
                Math.max(140, list.getPreferredSize().width + 24)), 100));
        setLeftComponent(scroll);
        setRightComponent(tabs);
        setBorder(javax.swing.BorderFactory.createEmptyBorder());
        setContinuousLayout(true);
        setResizeWeight(0d);
        setDividerLocation(scroll.getPreferredSize().width);
        synchronize();
    }

    private void synchronize() {
        if (updating) {
            return;
        }
        updating = true;
        try {
            model.clear();
            for (int i = 0; i < tabs.getTabCount(); ++i) {
                model.addElement(tabs.getTitleAt(i));
            }
            list.setSelectedIndex(tabs.getSelectedIndex());
            list.ensureIndexIsVisible(list.getSelectedIndex());
        } finally {
            updating = false;
        }
    }

    private static final class PageUI extends BasicTabbedPaneUI {
        @Override protected int calculateTabAreaHeight(int placement, int runs, int height) { return 0; }
        @Override protected int calculateTabAreaWidth(int placement, int runs, int width) { return 0; }
        @Override protected Insets getContentBorderInsets(int placement) { return new Insets(0, 0, 0, 0); }
        @Override protected void paintTabArea(Graphics g, int placement, int selected) {}
        @Override protected void paintContentBorder(Graphics g, int placement, int selected) {}
    }
}
