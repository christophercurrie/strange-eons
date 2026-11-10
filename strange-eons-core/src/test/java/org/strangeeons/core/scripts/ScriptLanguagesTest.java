package org.strangeeons.core.scripts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.cgjennings.layout.TextStyle;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.strangeeons.core.Host;
import org.strangeeons.core.settings.SettingValues;

class ScriptLanguagesTest {

    @AfterEach
    void resetFake() {
        FakeScriptLanguage.id = "fake";
        synchronized (FakeScriptLanguage.evaluated) {
            FakeScriptLanguage.evaluated.clear();
        }
    }

    @Test
    void findsRegisteredLanguagesById() {
        assertThat(ScriptLanguages.find("fake")).isInstanceOf(FakeScriptLanguage.class);
        assertThat(ScriptLanguages.find("missing")).isNull();
        assertThat(ScriptLanguages.all()).hasAtLeastOneElementOfType(FakeScriptLanguage.class);
        assertThatThrownBy(() -> ScriptLanguages.find(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void searchesItsOwnLoaderWhenTheContextLoaderDiffers() throws Exception {
        Thread t = Thread.currentThread();
        ClassLoader saved = t.getContextClassLoader();
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            t.setContextClassLoader(empty);
            assertThat(ScriptLanguages.find("fake")).isNotNull();
            t.setContextClassLoader(null);
            assertThat(ScriptLanguages.find("fake")).isNotNull();
        } finally {
            t.setContextClassLoader(saved);
        }
        assertThat(ScriptLanguages.all().stream().filter(l -> l instanceof FakeScriptLanguage)).hasSize(1);
    }

    @Test
    void styleLiteralsRunInTheJavaScriptLanguage() {
        FakeScriptLanguage.id = ScriptLanguages.JAVASCRIPT;
        int closedBefore = FakeScriptLanguage.closed;
        Host host = new Host() {
        };
        assertThat(host.evaluateStyleLiteral("6 * 2")).isEqualTo(12);
        assertThat(FakeScriptLanguage.evaluated).hasSize(2);
        assertThat(FakeScriptLanguage.evaluated.get(0)).contains("importClass(java.awt.font.TextAttribute)");
        assertThat(FakeScriptLanguage.evaluated.get(1)).isEqualTo("6 * 2");
        assertThat(FakeScriptLanguage.closed).isEqualTo(closedBefore + 1);

        TextStyle style = SettingValues.textStyle("size: {3 * 4}", null);
        assertThat(style.get(TextAttribute.SIZE)).isEqualTo(12);

        assertThatThrownBy(() -> host.evaluateStyleLiteral("fail"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("{fail}");
    }

    @Test
    void styleLiteralsNeedAJavaScriptLanguage() {
        assertThatThrownBy(() -> new Host() {
        }.evaluateStyleLiteral("1 * 1")).isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("JavaScript language");
    }
}
