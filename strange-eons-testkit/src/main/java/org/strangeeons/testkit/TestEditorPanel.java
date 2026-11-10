package org.strangeeons.testkit;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;
import org.strangeeons.core.editor.SettingBinding;
import org.strangeeons.core.editor.SwingValue;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.ComponentHandler;
import org.strangeeons.plugins.conventional.EditorControls;
import org.strangeeons.plugins.conventional.EditorPanel;
import org.strangeeons.plugins.observable.MutableObservable;

/**
 * An {@link EditorPanel} for testing a component handler's editor without the
 * application. Tests build the handler's interface, change controls, and check
 * the settings and the faces marked as changed. No window is needed: the
 * controls are ordinary Swing components that can be laid out and painted
 * headless.
 *
 * <pre>
 * TestSettings settings = new TestSettings();
 * TestEditorPanel panel = TestEditorPanel.build(new MyHandler(), settings);
 * panel.find("name", JTextField.class).setText("Gloin");
 * assertEquals("Gloin", settings.get("name"));
 * assertEquals(Set.of(0), panel.getChangedFaces());
 * </pre>
 */
public final class TestEditorPanel implements EditorPanel {

    private final SettingsReadWrite settings;
    private final int faceCount;
    private final JTabbedPane tabs = new JTabbedPane();
    private final EditorControls controls = new PlainEditorControls();
    private final List<SettingBinding> bindings = new ArrayList<>();
    private final List<Runnable> populateActions = new ArrayList<>();
    private final Set<Integer> changed = new TreeSet<>();
    private JTextComponent nameField;

    /**
     * Creates an empty panel.
     *
     * @param settings the settings that controls are bound to
     * @param faceCount the number of faces the component has
     */
    public TestEditorPanel(SettingsReadWrite settings, int faceCount) {
        this.settings = Objects.requireNonNull(settings, "settings");
        if (faceCount < 1) {
            throw new IllegalArgumentException("faceCount must be at least 1: " + faceCount);
        }
        this.faceCount = faceCount;
    }

    /**
     * Creates a component's settings and editor as the application would:
     * calls the handler's {@code create} and {@code createInterface}, then
     * {@linkplain #populate populates} the controls.
     *
     * @param handler the handler under test
     * @param settings the component's settings
     * @return the populated panel
     */
    public static TestEditorPanel build(ComponentHandler handler, SettingsReadWrite settings) {
        Objects.requireNonNull(handler, "handler");
        handler.create(settings);
        TestEditorPanel panel = new TestEditorPanel(settings, handler.getFaceCount());
        handler.createInterface(settings, panel);
        panel.populate();
        return panel;
    }

    @Override
    public void addTab(String title, JComponent content) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        content.setName(title);
        tabs.addTab(title, content);
    }

    @Override
    public void bind(String key, JComponent control, int... faces) {
        bind(key, SwingValue.of(control), faces);
    }

    @Override
    public void bind(String key, MutableObservable<String> value, int... faces) {
        bindings.add(new SettingBinding(key, value, () -> settings, () -> faceCount, changed::add, faces));
    }

    @Override
    public void bindGroup(String key, List<? extends AbstractButton> buttons, List<String> values, int... faces) {
        bind(key, SwingValue.ofGroup(buttons, values), faces);
    }

    @Override
    public void setNameField(JTextComponent field) {
        nameField = field;
    }

    @Override
    public void onPopulate(Runnable action) {
        populateActions.add(Objects.requireNonNull(action, "action"));
    }

    @Override
    public void markChanged(int face) {
        changed.add(face);
    }

    @Override
    public EditorControls getControls() {
        return controls;
    }

    /**
     * Updates every bound control from the settings, then runs the populate
     * actions, as the application does when an editor is opened or its
     * component changes.
     */
    public void populate() {
        bindings.forEach(SettingBinding::populate);
        populateActions.forEach(Runnable::run);
    }

    /**
     * Returns the tabs added to the panel.
     *
     * @return the tabbed pane holding the editor's tabs
     */
    public JTabbedPane getTabs() {
        return tabs;
    }

    /**
     * Returns the titles of the tabs, in order.
     *
     * @return the tab titles
     */
    public List<String> getTabTitles() {
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); ++i) {
            titles.add(tabs.getTitleAt(i));
        }
        return titles;
    }

    /**
     * Returns the field set as the component name field.
     *
     * @return the name field, or {@code null}
     */
    public JTextComponent getNameField() {
        return nameField;
    }

    /**
     * Returns the faces marked as changed since the panel was created or
     * {@linkplain #clearChanges cleared}.
     *
     * @return the changed face indices, in order
     */
    public Set<Integer> getChangedFaces() {
        return Collections.unmodifiableSet(new TreeSet<>(changed));
    }

    /** Forgets which faces were marked as changed. */
    public void clearChanges() {
        changed.clear();
    }

    /**
     * Finds a control by its component name in any tab.
     *
     * @param <T> the control type
     * @param name the name given with {@link Component#setName}
     * @param type the expected control type
     * @return the control
     * @throws IllegalArgumentException if no control of that type has the name
     */
    public <T extends Component> T find(String name, Class<T> type) {
        Component c = find(tabs, Objects.requireNonNull(name, "name"), type);
        if (c == null) {
            throw new IllegalArgumentException("no " + type.getSimpleName() + " named \"" + name + '"');
        }
        return type.cast(c);
    }

    private static Component find(Component c, String name, Class<?> type) {
        if (name.equals(c.getName()) && type.isInstance(c)) {
            return c;
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                Component found = find(child, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Paints a tab's content, laid out at the given size.
     *
     * @param title the tab title
     * @param width the width, in pixels
     * @param height the height, in pixels
     * @return an image of the tab content
     * @throws IllegalArgumentException if there is no tab with the title
     */
    public BufferedImage paintTab(String title, int width, int height) {
        int index = tabs.indexOfTab(title);
        if (index < 0) {
            throw new IllegalArgumentException("no tab titled \"" + title + '"');
        }
        Component content = tabs.getComponentAt(index);
        content.setSize(width, height);
        layout(content);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            content.printAll(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void layout(Component c) {
        c.doLayout();
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                layout(child);
            }
        }
    }
}
