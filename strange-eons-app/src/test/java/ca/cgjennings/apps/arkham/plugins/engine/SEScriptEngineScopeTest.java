package ca.cgjennings.apps.arkham.plugins.engine;

import static org.assertj.core.api.Assertions.assertThat;

import javax.script.ScriptEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.strangeeons.core.scripts.ScriptLanguages;
import org.strangeeons.scripts.ScriptSession;

class SEScriptEngineScopeTest {

    private static boolean warningsEnabled;

    // implicit globals raise warnings, which are written to the app console
    @BeforeAll
    static void disableWarnings() {
        warningsEnabled = SEScriptEngineFactory.getWarningsEnabled();
        SEScriptEngineFactory.setWarningsEnabled(false);
    }

    @AfterAll
    static void restoreWarnings() {
        SEScriptEngineFactory.setWarningsEnabled(warningsEnabled);
    }

    private static ScriptSession newSession() {
        return ScriptLanguages.find(ScriptLanguages.JAVASCRIPT).createSession();
    }

    private static String eval(ScriptSession session, String script) throws Exception {
        return String.valueOf(session.eval(script, "scope test"));
    }

    @Test
    void evalSeesTopLevelVar() throws Exception {
        try (ScriptSession session = newSession()) {
            assertThat(eval(session, "var x = 'ok'; eval('x');")).isEqualTo("ok");
        }
    }

    @Test
    void evalInFunctionSeesTopLevelVar() throws Exception {
        try (ScriptSession session = newSession()) {
            assertThat(eval(session,
                    "var obj = { icon: 'ok' }; function f() { return eval('obj.' + 'icon'); } f();"))
                    .isEqualTo("ok");
        }
    }

    @Test
    void evalInCalledFunctionSeesVarFromEarlierEval() throws Exception {
        try (ScriptSession session = newSession()) {
            session.eval("var obj = { icon: 'ok' };", "scope test");
            session.eval("function f() { return eval('obj.icon'); }", "scope test");
            assertThat(String.valueOf(session.call("f"))).isEqualTo("ok");
        }
    }

    @Test
    void evalSeesImplicitGlobal() throws Exception {
        try (ScriptSession session = newSession()) {
            session.eval("function init() { implicitGlobal = 'ok'; } init();", "scope test");
            assertThat(eval(session, "function f() { return eval('implicitGlobal'); } f();"))
                    .isEqualTo("ok");
        }
    }

    @Test
    void functionConstructorSeesTopLevelVar() throws Exception {
        try (ScriptSession session = newSession()) {
            assertThat(eval(session, "var x = 'ok'; new Function('return x;')();")).isEqualTo("ok");
        }
    }

    @Test
    void importClassWorks() throws Exception {
        try (ScriptSession session = newSession()) {
            assertThat(eval(session,
                    "importClass(java.lang.StringBuilder); new StringBuilder('ok').toString();"))
                    .isEqualTo("ok");
        }
    }

    @Test
    void importPackageWorks() throws Exception {
        try (ScriptSession session = newSession()) {
            assertThat(eval(session,
                    "importPackage(java.util.concurrent); new ConcurrentHashMap().getClass().getSimpleName();"))
                    .isEqualTo("ConcurrentHashMap");
        }
    }

    @Test
    void importClassDoesNotLeakToOtherSessions() throws Exception {
        try (ScriptSession a = newSession(); ScriptSession b = newSession()) {
            a.eval("importClass(java.util.concurrent.atomic.AtomicLong);", "scope test");
            assertThat(eval(b, "typeof AtomicLong;")).isEqualTo("undefined");
        }
    }

    @Test
    void importPackageDoesNotLeakToOtherSessions() throws Exception {
        try (ScriptSession a = newSession(); ScriptSession b = newSession()) {
            a.eval("importPackage(java.util.concurrent.locks);", "scope test");
            assertThat(eval(b, "typeof ReentrantLock;")).isEqualTo("undefined");
        }
    }

    @Test
    void importFunctionsKeepTheirNames() throws Exception {
        try (ScriptSession session = newSession()) {
            assertThat(eval(session, "[importClass.name, importPackage.name].join();"))
                    .isEqualTo("importClass,importPackage");
        }
    }

    @Test
    void importClassRejectsNonClass() throws Exception {
        try (ScriptSession session = newSession()) {
            assertThat(eval(session, "try { importClass('not a class'); 'imported'; } catch (e) { 'rejected'; }"))
                    .isEqualTo("rejected");
        }
    }

    @Test
    void importPackageRejectsNonPackage() throws Exception {
        try (ScriptSession session = newSession()) {
            assertThat(eval(session, "try { importPackage('not a package'); 'imported'; } catch (e) { 'rejected'; }"))
                    .isEqualTo("rejected");
        }
    }

    // Engine internals: sessions do not expose their bindings.
    @Test
    void implicitGlobalIsStoredInEngineBindings() throws Exception {
        ScriptEngine engine = new SEScriptEngineFactory().getScriptEngine();
        engine.eval("function init() { implicitGlobal = 'ok'; } init();");
        assertThat(engine.get("implicitGlobal")).hasToString("ok");
    }
}
