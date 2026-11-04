package ca.cgjennings.apps.arkham.plugins.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.UndeclaredThrowableException;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import org.junit.jupiter.api.Test;

class EngineUtilitiesImplementTest {

    public interface Hooks {
        String required();

        default String optional() {
            return "default";
        }
    }

    private static Hooks implement(String source) throws Exception {
        ScriptEngine engine = new SEScriptEngineFactory().getScriptEngine();
        engine.eval(source);
        return ((Invocable) engine).getInterface(Hooks.class);
    }

    @Test
    void scriptFunctionOverridesDefault() throws Exception {
        Hooks hooks = implement("function required() { return 'r'; } function optional() { return 'o'; }");

        assertThat(hooks.required()).isEqualTo("r");
        assertThat(hooks.optional()).isEqualTo("o");
    }

    @Test
    void missingFunctionFallsBackToDefault() throws Exception {
        assertThat(implement("function required() { return 'r'; }").optional()).isEqualTo("default");
    }

    @Test
    void missingFunctionWithoutDefaultStillFails() throws Exception {
        Hooks hooks = implement("");

        assertThatThrownBy(hooks::required)
                .isInstanceOf(UndeclaredThrowableException.class)
                .hasCauseInstanceOf(NoSuchMethodException.class);
    }
}
