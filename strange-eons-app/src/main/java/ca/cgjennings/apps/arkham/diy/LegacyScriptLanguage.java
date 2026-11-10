package ca.cgjennings.apps.arkham.diy;

import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import java.io.IOException;
import java.util.Set;
import org.strangeeons.scripts.ScriptException;
import org.strangeeons.scripts.ScriptLanguage;
import org.strangeeons.scripts.ScriptSession;
import resources.Settings;

/**
 * Runs {@code .js} handler scripts through {@link ScriptMonkey} for one
 * component. Not registered with {@link java.util.ServiceLoader}: sessions read
 * the component's settings through {@code $} notation, so each component has
 * its own instance.
 */
final class LegacyScriptLanguage implements ScriptLanguage {

    private final Settings settings;
    private final boolean debug;

    LegacyScriptLanguage(Settings settings, boolean debug) {
        this.settings = settings;
        this.debug = debug;
    }

    @Override
    public String id() {
        return "javascript";
    }

    @Override
    public String displayName() {
        return "JavaScript";
    }

    @Override
    public Set<String> fileExtensions() {
        return Set.of(".js");
    }

    @Override
    public ScriptSession createSession() {
        return new Session();
    }

    private final class Session implements ScriptSession {

        private ScriptMonkey monkey;

        @Override
        public <T> T createHandler(String spec, Class<T> type) throws ScriptException {
            String script = spec.contains(":/") ? spec : "res://" + spec;
            String source;
            try {
                source = ScriptMonkey.getLibrary(script);
            } catch (IOException e) {
                throw new ScriptException(script, e);
            }
            eval(source, script);
            try {
                return monkey.implement(type);
            } catch (IllegalArgumentException e) {
                throw new ScriptException("DIY script does not implement Handler functions: " + script, e);
            }
        }

        @Override
        public Object eval(String source, String sourceName) throws ScriptException {
            if (monkey == null) {
                monkey = new ScriptMonkey(sourceName);
                monkey.setSettingProvider(settings);
                monkey.bind(PluginContextFactory.createDummyContext());
                monkey.setBreakpoint(debug);
            }
            return checked(monkey.eval(source), sourceName);
        }

        @Override
        public Object call(String name, Object... args) throws ScriptException {
            requireMonkey(name);
            return checked(monkey.call(name, args), name);
        }

        @Override
        public Object callIfDefined(String name, Object... args) throws ScriptException {
            requireMonkey(name);
            return checked(monkey.ambivalentCall(name, args), name);
        }

        @Override
        public void bind(String name, Object value) {
            if (monkey == null) {
                throw new IllegalStateException("no script evaluated yet: " + name);
            }
            monkey.bind(name, value);
        }

        /**
         * Empties the engine's bindings instead of relying on collection:
         * Java proxies registered on long-lived Swing components keep the
         * engine reachable after the editor closes.
         */
        @Override
        public void close() {
            if (monkey == null) {
                return;
            }
            try {
                javax.script.Bindings b = monkey.getEngine().getBindings(javax.script.ScriptContext.ENGINE_SCOPE);
                if (b != null) {
                    b.clear();
                }
            } catch (Throwable t) {
                // best effort: the engine's footprint just stays large
            }
            monkey = null;
        }

        private void requireMonkey(String name) throws ScriptException {
            if (monkey == null) {
                throw new ScriptException("no script evaluated yet: " + name);
            }
        }

        /** {@link ScriptMonkey} reports script errors and returns them as values. */
        private Object checked(Object result, String where) throws ScriptException {
            if (result instanceof Throwable t) {
                throw new ScriptException(where, t);
            }
            return result;
        }
    }
}
