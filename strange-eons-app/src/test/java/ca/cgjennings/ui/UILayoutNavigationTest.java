package ca.cgjennings.ui;

import java.awt.Component;
import java.nio.charset.StandardCharsets;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import static org.assertj.core.api.Assertions.assertThat;

class UILayoutNavigationTest {
    @Test void scriptsAddPagesWithEitherNavigationAndDesignSupport() throws Exception {
        String source;
        try (var in = getClass().getResourceAsStream("/resources/libraries/uilayout.js")) {
            source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        SwingUtilities.invokeAndWait(() -> {
            try (Context cx = Context.enter()) {
                cx.setLanguageVersion(Context.VERSION_ES6);
                var scope = cx.initStandardObjects();
                cx.evaluateString(scope, """
                        var swing = Packages.javax.swing;
                        Function.prototype.subclass = function(base) {
                            this.prototype = Object.create(base.prototype);
                            this.prototype.constructor = this;
                        };
                        """, "test", 1, null);
                cx.evaluateString(scope, source, "uilayout", 1, null);
                for (boolean side : new boolean[]{false, true}) {
                    for (boolean support : new boolean[]{false, true}) {
                        JTabbedPane tabs = new JTabbedPane();
                        tabs.addTab("Comments", new JPanel());
                        Component controls = side ? new TabbedPaneNavigator(tabs) : tabs;
                        if (support) controls = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controls, new JPanel());
                        JSplitPane preview = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, new JPanel());
                        JPanel content = new JPanel();
                        content.add(preview);
                        ScriptableObject.putProperty(scope, "content", Context.javaToJS(content, scope));
                        Object found = cx.evaluateString(scope, """
                                var editor = {getContentPane: function() {return content;}, showing: false};
                                var container = new AbstractContainer();
                                container.realize = function() { return new swing.JPanel(); };
                                container.addToEditor(editor, 'General');
                                AbstractContainer.findEditorTabPane(editor);
                                """, "test", 1, null);

                        assertThat(Context.jsToJava(found, JTabbedPane.class)).isSameAs(tabs);
                        assertThat(tabs.getTabCount()).isEqualTo(2);
                        assertThat(tabs.getTitleAt(0)).isEqualTo("General");
                        assertThat(tabs.getTitleAt(1)).isEqualTo("Comments");

                        if (side) {
                            TabbedPaneNavigator nav = (TabbedPaneNavigator) tabs.getParent();

                            assertThat(nav.getDividerLocation()).isGreaterThanOrEqualTo(140);

                            JList<?> list = (JList<?>) ((JScrollPane) nav.getLeftComponent()).getViewport().getView();

                            assertThat(list.getModel().getElementAt(0)).isEqualTo("General");
                        }
                    }
                }
            }
        });
    }
}
