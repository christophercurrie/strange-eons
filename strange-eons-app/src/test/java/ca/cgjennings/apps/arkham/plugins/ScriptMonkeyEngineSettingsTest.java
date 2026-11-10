package ca.cgjennings.apps.arkham.plugins;

import static org.assertj.core.api.Assertions.assertThat;

import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngineFactory;
import org.junit.jupiter.api.Test;
import org.strangeeons.core.scripts.ScriptLanguages;
import org.strangeeons.scripts.ScriptSession;
import resources.Settings;

class ScriptMonkeyEngineSettingsTest {

    @Test
    void evaluatesWithoutApplicationStartup() throws Exception {
        try (ScriptSession session = ScriptLanguages.find(ScriptLanguages.JAVASCRIPT).createSession()) {
            assertThat(((Number) session.eval("6 * 2", "unit test")).intValue()).isEqualTo(12);
        }
    }

    @Test
    void appliesAndClampsUserEngineSettings() {
        Settings user = Settings.getUser();
        try {
            user.set("script-optimization-level", "42");
            user.set("script-warnings", "yes");
            ScriptMonkey.applyEngineSettings();
            assertThat(SEScriptEngineFactory.getOptimizationLevel()).isEqualTo(9);
            assertThat(SEScriptEngineFactory.getWarningsEnabled()).isTrue();

            user.set("script-optimization-level", "-5");
            user.set("script-warnings", "no");
            ScriptMonkey.applyEngineSettings();
            assertThat(SEScriptEngineFactory.getOptimizationLevel()).isEqualTo(-1);
            assertThat(SEScriptEngineFactory.getWarningsEnabled()).isFalse();
        } finally {
            user.reset("script-optimization-level");
            user.reset("script-warnings");
            ScriptMonkey.applyEngineSettings();
        }
        assertThat(SEScriptEngineFactory.getOptimizationLevel()).isEqualTo(0);
    }
}
