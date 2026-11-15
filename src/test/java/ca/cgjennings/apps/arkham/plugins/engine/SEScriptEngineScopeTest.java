package ca.cgjennings.apps.arkham.plugins.engine;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static ScriptEngine newEngine() {
        return new SEScriptEngineFactory().getScriptEngine();
    }

    private static String eval(ScriptEngine engine, String script) throws ScriptException {
        return String.valueOf(engine.eval(script));
    }

    @Test
    void evalSeesTopLevelVar() throws Exception {
        ScriptEngine engine = newEngine();
        assertEquals("ok", eval(engine, "var x = 'ok'; eval('x');"));
    }

    @Test
    void evalInFunctionSeesTopLevelVar() throws Exception {
        ScriptEngine engine = newEngine();
        assertEquals("ok", eval(engine,
                "var obj = { icon: 'ok' }; function f() { return eval('obj.' + 'icon'); } f();"));
    }

    @Test
    void evalInInvokedFunctionSeesVarFromEarlierEval() throws Exception {
        ScriptEngine engine = newEngine();
        engine.eval("var obj = { icon: 'ok' };");
        engine.eval("function f() { return eval('obj.icon'); }");
        assertEquals("ok", String.valueOf(((Invocable) engine).invokeFunction("f")));
    }

    @Test
    void evalSeesImplicitGlobal() throws Exception {
        ScriptEngine engine = newEngine();
        engine.eval("function init() { implicitGlobal = 'ok'; } init();");
        assertEquals("ok", eval(engine, "function f() { return eval('implicitGlobal'); } f();"));
    }

    @Test
    void implicitGlobalIsStoredInEngineBindings() throws Exception {
        ScriptEngine engine = newEngine();
        engine.eval("function init() { implicitGlobal = 'ok'; } init();");
        assertEquals("ok", String.valueOf(engine.get("implicitGlobal")));
    }

    @Test
    void functionConstructorSeesTopLevelVar() throws Exception {
        ScriptEngine engine = newEngine();
        assertEquals("ok", eval(engine, "var x = 'ok'; new Function('return x;')();"));
    }

    @Test
    void importClassWorks() throws Exception {
        ScriptEngine engine = newEngine();
        assertEquals("ok", eval(engine,
                "importClass(java.lang.StringBuilder); new StringBuilder('ok').toString();"));
    }

    @Test
    void importPackageWorks() throws Exception {
        ScriptEngine engine = newEngine();
        assertNotNull(engine.eval(
                "importPackage(java.util.concurrent); new ConcurrentHashMap();"));
    }

    @Test
    void importClassDoesNotLeakToOtherEngines() throws Exception {
        newEngine().eval("importClass(java.util.concurrent.atomic.AtomicLong);");
        assertEquals("undefined", eval(newEngine(), "typeof AtomicLong;"));
    }

    @Test
    void importPackageDoesNotLeakToOtherEngines() throws Exception {
        newEngine().eval("importPackage(java.util.concurrent.locks);");
        assertEquals("undefined", eval(newEngine(), "typeof ReentrantLock;"));
    }

    @Test
    void importFunctionsKeepTheirNames() throws Exception {
        ScriptEngine engine = newEngine();
        assertEquals("importClass,importPackage", eval(engine, "[importClass.name, importPackage.name].join();"));
    }

    @Test
    void importClassRejectsNonClass() {
        ScriptEngine engine = newEngine();
        assertThrows(ScriptException.class, () -> engine.eval("importClass('not a class');"));
    }

    @Test
    void importPackageRejectsNonPackage() {
        ScriptEngine engine = newEngine();
        assertThrows(ScriptException.class, () -> engine.eval("importPackage('not a package');"));
    }
}
