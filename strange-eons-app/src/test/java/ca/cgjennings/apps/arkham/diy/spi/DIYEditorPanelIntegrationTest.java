package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYEditor;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.ComponentHandler;
import org.strangeeons.plugins.conventional.EditorPanel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs a handler's editor through a real {@link DIY}: building the
 * interface, editing bound controls, and populating from changed settings.
 */
class DIYEditorPanelIntegrationTest {

    private final JTextField name = new JTextField();
    private final JTextField rules = new JTextField("default rules");
    private final JCheckBox unique = new JCheckBox();
    private final JRadioButton light = new JRadioButton();
    private final JRadioButton dark = new JRadioButton();
    private final JTabbedPane tabs = new JTabbedPane();
    private final DIYEditor editor = mock(DIYEditor.class);
    private final String[] populatedName = new String[1];

    private DIY diy;

    private final ComponentHandler handler = new ComponentHandler() {
        @Override
        public void createInterface(SettingsReadWrite settings, EditorPanel panel) {
            panel.bind("Name", name, 0);
            panel.bind("Rules", rules, 0);
            panel.bind("Unique", unique, 0);
            panel.bindGroup("Side", List.of(light, dark), List.of("light", "dark"), 0, 1);
            panel.onPopulate(() -> populatedName[0] = name.getText());
            panel.addTab("Details", new JPanel());
        }
    };

    @BeforeEach
    void setUp() throws Exception {
        diy = new DIY(TestComponentHandler.class.getName(), null, false);
        light.setSelected(true);
        when(editor.getTabbedPane()).thenReturn(tabs);
        new ComponentHandlerBridge(handler).createInterface(diy, editor);
        diy.markSaved();
    }

    private void populate() {
        ArgumentCaptor<StrangeEonsEditor.FieldPopulationListener> listener
                = ArgumentCaptor.forClass(StrangeEonsEditor.FieldPopulationListener.class);
        verify(editor).addFieldPopulationListener(listener.capture());
        listener.getValue().fieldsPopulated(editor);
    }

    @Test
    void buildingTheInterfaceSeedsDefaultsAndAddsTab() {
        assertThat(diy.getSettings().get("Rules")).isEqualTo("default rules");
        assertThat(diy.getSettings().get("Unique")).isEqualTo("0");
        assertThat(diy.getSettings().get("Side")).isEqualTo("light");
        assertThat(tabs.getTitleAt(0)).isEqualTo("Details");
    }

    @Test
    void editingAControlUpdatesSettingAndMarksUnsaved() {
        name.setText("Luke");

        assertThat(diy.getSettings().get("Name")).isEqualTo("Luke");
        assertThat(diy.hasUnsavedChanges()).isTrue();
    }

    @Test
    void clickingAGroupButtonUpdatesSetting() {
        dark.doClick();

        assertThat(diy.getSettings().get("Side")).isEqualTo("dark");
        assertThat(diy.hasUnsavedChanges()).isTrue();
    }

    @Test
    void populateUpdatesControlsWithoutMarkingUnsaved() {
        diy.getSettings().set("Name", "Leia");
        diy.getSettings().set("Unique", "1");
        diy.getSettings().set("Side", "dark");

        populate();

        assertThat(name.getText()).isEqualTo("Leia");
        assertThat(unique.isSelected()).isTrue();
        assertThat(dark.isSelected()).isTrue();
        assertThat(populatedName[0]).isEqualTo("Leia");
        assertThat(diy.hasUnsavedChanges()).isFalse();
    }

    @Test
    void marksEachListedSheetWhenSheetsExist() throws Exception {
        Sheet<?>[] sheets = {mock(Sheet.class), mock(Sheet.class)};
        diy.setSheets(sheets);

        dark.doClick();

        verify(sheets[0]).markChanged();
        verify(sheets[1]).markChanged();
    }
}
