package org.strangeeons.core.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.strangeeons.scripts.ScriptException;
import org.strangeeons.scripts.ScriptLanguage;
import org.strangeeons.scripts.ScriptSession;

/**
 * A test language that evaluates integer arithmetic of the form {@code a * b}
 * and records what it was asked to run. Its id can be changed so tests can
 * stand it in for another language.
 */
public final class FakeScriptLanguage implements ScriptLanguage {

    public static volatile String id = "fake";
    public static final List<String> evaluated = new ArrayList<>();
    public static volatile int closed;

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return "Fake";
    }

    @Override
    public Set<String> fileExtensions() {
        return Set.of(".fake");
    }

    @Override
    public ScriptSession createSession() {
        return new ScriptSession() {
            @Override
            public Object eval(String source, String sourceName) throws ScriptException {
                synchronized (evaluated) {
                    evaluated.add(source.trim());
                }
                if (source.contains("fail")) {
                    throw new ScriptException("failed: " + source);
                }
                String[] parts = source.split("\\*");
                if (parts.length != 2) {
                    return null;
                }
                return Integer.parseInt(parts[0].trim()) * Integer.parseInt(parts[1].trim());
            }

            @Override
            public Object call(String name, Object... args) {
                return null;
            }

            @Override
            public Object callIfDefined(String name, Object... args) {
                return null;
            }

            @Override
            public void bind(String name, Object value) {
            }

            @Override
            public void close() {
                ++closed;
            }
        };
    }
}
