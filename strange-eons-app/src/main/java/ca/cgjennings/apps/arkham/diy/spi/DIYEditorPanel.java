package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYEditor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;
import org.strangeeons.plugins.conventional.EditorControls;
import org.strangeeons.plugins.conventional.EditorPanel;
import org.strangeeons.plugins.observable.MutableObservable;

/**
 * {@link EditorPanel} backed by a {@link DIYEditor}. The framework calls
 * {@link #attach()} after the handler's {@code createInterface} returns,
 * so that bindings and actions follow each later population of the
 * editor.
 */
public final class DIYEditorPanel implements EditorPanel {

    /** Client property the editor uses to identify added tabs. */
    static final String CUSTOM_TAB_PROPERTY = "se-custom-tab";

    private final DIY diy;
    private final DIYEditor editor;
    private final EditorControls controls = new DIYEditorControls();
    private final List<SettingBinding> bindings = new ArrayList<>();
    private final List<Runnable> populateActions = new ArrayList<>();

    public DIYEditorPanel(DIY diy, DIYEditor editor) {
        this.diy = Objects.requireNonNull(diy, "diy");
        this.editor = Objects.requireNonNull(editor, "editor");
    }

    @Override
    public void addTab(String title, JComponent content) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        JTabbedPane tabs = editor.getTabbedPane();
        content.putClientProperty(CUSTOM_TAB_PROPERTY, title);
        content.setName(title);
        tabs.add(content, Math.max(0, tabs.getTabCount() - 1));
        if (!editor.isShowing() && tabs.getParent() instanceof JSplitPane splitter) {
            splitter.setDividerLocation(-1);
        }
    }

    @Override
    public void bind(String key, JComponent control, int... faces) {
        bind(key, SwingValue.of(control), faces);
    }

    @Override
    public void bind(String key, MutableObservable<String> value, int... faces) {
        bindings.add(new SettingBinding(key, value, diy, faces));
    }

    @Override
    public void bindGroup(String key, List<? extends AbstractButton> buttons,
            List<String> values, int... faces) {
        bind(key, SwingValue.ofGroup(buttons, values), faces);
    }

    @Override
    public void setNameField(JTextComponent field) {
        diy.setNameField(field);
    }

    @Override
    public void onPopulate(Runnable action) {
        populateActions.add(Objects.requireNonNull(action, "action"));
    }

    @Override
    public void markChanged(int face) {
        diy.markChanged(face);
    }

    @Override
    public EditorControls getControls() {
        return controls;
    }

    /** Starts following populate events from the editor. */
    public void attach() {
        editor.addFieldPopulationListener((StrangeEonsEditor.FieldPopulationListener) source -> populate());
    }

    private void populate() {
        bindings.forEach(SettingBinding::populate);
        populateActions.forEach(Runnable::run);
    }
}
