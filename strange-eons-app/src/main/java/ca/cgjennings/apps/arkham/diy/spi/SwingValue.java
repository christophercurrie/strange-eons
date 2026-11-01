package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.plugins.UILibraryHelper;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import org.strangeeons.plugins.observable.MutableObservable;
import resources.Settings;

/**
 * Adapts a Swing control to a {@link MutableObservable} of its setting
 * value. Notifications are pushed as the control changes, so a control
 * can be bound without polling.
 */
final class SwingValue implements MutableObservable<String> {

    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private final CopyOnWriteArrayList<Consumer<? super String>> listeners
            = new CopyOnWriteArrayList<>();

    private SwingValue(Supplier<String> getter, Consumer<String> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    /**
     * Returns the value of a standard control.
     *
     * @throws IllegalArgumentException if the control has no standard
     *     binding
     */
    static SwingValue of(JComponent control) {
        Objects.requireNonNull(control, "control");
        if (control instanceof JScrollPane scroll
                && scroll.getViewport().getView() instanceof JComponent view) {
            return of(view);
        }
        if (control instanceof JTextComponent text) {
            return ofText(text);
        }
        if (control instanceof JToggleButton toggle) {
            return ofToggle(toggle);
        }
        if (control instanceof JComboBox<?> combo) {
            return combo.isEditable() ? ofEditableCombo(combo) : ofCombo(combo);
        }
        throw new IllegalArgumentException(
                "no standard binding for " + control.getClass().getName());
    }

    /** Returns the value of a group of buttons mapped to setting values. */
    static SwingValue ofGroup(List<? extends AbstractButton> buttons, List<String> values) {
        UILibraryHelper.BindableGroup group = new UILibraryHelper.BindableGroup(
                buttons.toArray(new AbstractButton[0]), values.toArray(new String[0]));
        SwingValue value = new SwingValue(
                () -> values.stream()
                        .filter(v -> buttons.get(values.indexOf(v)).isSelected())
                        .findFirst().orElse(null),
                v -> {
                    if (values.contains(v)) {
                        group.fromSetting(v);
                    }
                });
        group.addActionListener(e -> value.fire());
        return value;
    }

    private static SwingValue ofText(JTextComponent text) {
        SwingValue value = new SwingValue(text::getText, v -> {
            text.setText(v);
            text.select(0, 0);
        });
        value.listenTo(text);
        return value;
    }

    private static SwingValue ofToggle(JToggleButton toggle) {
        SwingValue value = new SwingValue(
                () -> toggle.isSelected() ? "1" : "0",
                v -> toggle.setSelected(Settings.yesNo(v)));
        toggle.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED
                    || e.getStateChange() == ItemEvent.DESELECTED) {
                value.fire();
            }
        });
        return value;
    }

    private static SwingValue ofCombo(JComboBox<?> combo) {
        SwingValue value = new SwingValue(
                () -> Objects.toString(combo.getSelectedItem(), null),
                v -> {
                    ComboBoxModel<?> model = combo.getModel();
                    for (int i = 0; i < model.getSize(); ++i) {
                        if (String.valueOf(model.getElementAt(i)).equals(v)) {
                            combo.setSelectedIndex(i);
                            return;
                        }
                    }
                });
        combo.addActionListener(e -> value.fire());
        return value;
    }

    private static SwingValue ofEditableCombo(JComboBox<?> combo) {
        if (!(combo.getEditor().getEditorComponent() instanceof JTextComponent editor)) {
            throw new IllegalArgumentException("combo box editor is not a text component");
        }
        SwingValue value = new SwingValue(editor::getText, combo::setSelectedItem);
        value.listenTo(editor);
        return value;
    }

    private void listenTo(JTextComponent text) {
        text.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fire();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fire();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fire();
            }
        });
    }

    private void fire() {
        String current = get();
        for (Consumer<? super String> listener : listeners) {
            listener.accept(current);
        }
    }

    @Override
    public String get() {
        return getter.get();
    }

    @Override
    public void set(String value) {
        if (!Objects.equals(get(), value)) {
            setter.accept(value);
        }
    }

    @Override
    public AutoCloseable subscribe(Consumer<? super String> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
