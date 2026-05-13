package ca.cgjennings.apps.arkham.diy.spi;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.strangeeons.plugins.conventional.EditorPanel;

/**
 * Swing-backed {@link EditorPanel} implementation that surfaces a
 * {@link ComponentHandler}'s editor controls through the legacy
 * {@code DIYEditor}'s tabbed pane.
 *
 * <p>Plug-ins call {@link #addControl(String, Object)} with AWT
 * {@link Component} instances; the panel buffers the entries and
 * arranges them in a two-column label/control grid when
 * {@link #attach()} is invoked by the framework after the handler's
 * {@code createInterface} returns. A non-{@code Component} control
 * fails fast with {@link IllegalArgumentException}.
 */
public final class DIYEditorPanel implements EditorPanel {

    /** Title used for the editor tab the framework attaches. */
    public static final String TAB_TITLE = "Plug-in";

    /** Client property the editor uses to identify SPI-added tabs. */
    static final String CUSTOM_TAB_PROPERTY = "se-custom-tab";

    private final JTabbedPane tabbedPane;
    private final List<Entry> entries = new ArrayList<>();

    public DIYEditorPanel(JTabbedPane tabbedPane) {
        this.tabbedPane = Objects.requireNonNull(tabbedPane, "tabbedPane");
    }

    @Override
    public void addControl(String label, Object control) {
        Objects.requireNonNull(label, "label");
        if (!(control instanceof Component c)) {
            throw new IllegalArgumentException(
                    "DIY editor binding requires an AWT Component, got: "
                    + (control == null ? "null" : control.getClass().getName()));
        }
        entries.add(new Entry(label, c));
    }

    /** Returns the number of controls collected via {@link #addControl}. */
    public int getControlCount() {
        return entries.size();
    }

    /**
     * Builds a labeled grid from the collected entries and attaches
     * it to the underlying tabbed pane. Does nothing if no controls
     * were added.
     */
    public void attach() {
        if (entries.isEmpty()) {
            return;
        }

        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 4, 4, 8);

        GridBagConstraints controlGbc = new GridBagConstraints();
        controlGbc.gridx = 1;
        controlGbc.anchor = GridBagConstraints.WEST;
        controlGbc.fill = GridBagConstraints.HORIZONTAL;
        controlGbc.weightx = 1.0;
        controlGbc.insets = new Insets(4, 0, 4, 4);

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            labelGbc.gridy = i;
            controlGbc.gridy = i;
            grid.add(new JLabel(e.label), labelGbc);
            grid.add(e.control, controlGbc);
        }
        grid.putClientProperty(CUSTOM_TAB_PROPERTY, Boolean.TRUE);
        tabbedPane.add(TAB_TITLE, grid);
    }

    private record Entry(String label, Component control) {}
}
