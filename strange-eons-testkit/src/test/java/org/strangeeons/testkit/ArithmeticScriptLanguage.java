package org.strangeeons.testkit;

import java.util.Set;
import org.strangeeons.scripts.ScriptLanguage;
import org.strangeeons.scripts.ScriptSession;

/** Stands in for JavaScript: evaluates {@code a * b}, ignores other code. */
public final class ArithmeticScriptLanguage implements ScriptLanguage {

    @Override
    public String id() {
        return "javascript";
    }

    @Override
    public String displayName() {
        return "Arithmetic";
    }

    @Override
    public Set<String> fileExtensions() {
        return Set.of();
    }

    @Override
    public ScriptSession createSession() {
        return new ScriptSession() {
            @Override
            public Object eval(String source, String sourceName) {
                String[] parts = source.split("\\*");
                return parts.length == 2
                        ? Float.parseFloat(parts[0].trim()) * Float.parseFloat(parts[1].trim()) : null;
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
            }
        };
    }
}
