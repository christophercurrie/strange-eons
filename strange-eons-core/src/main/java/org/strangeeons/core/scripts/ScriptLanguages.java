package org.strangeeons.core.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import org.strangeeons.scripts.ScriptLanguage;

/**
 * Finds the script languages registered with {@link ServiceLoader}. Languages
 * are looked up through the current thread's context class loader, and then
 * through this class's loader if that is different.
 */
public final class ScriptLanguages {

    /** The id of the JavaScript language that style and other setting values use. */
    public static final String JAVASCRIPT = "javascript";

    private ScriptLanguages() {
    }

    /**
     * Returns the registered language with an id.
     *
     * @param id the language id, such as {@code "javascript"}
     * @return the language, or {@code null} if none is registered with the id
     */
    public static ScriptLanguage find(String id) {
        Objects.requireNonNull(id, "id");
        for (ScriptLanguage lang : all()) {
            if (id.equals(lang.id())) {
                return lang;
            }
        }
        return null;
    }

    /**
     * Returns every registered language.
     *
     * @return the registered languages, in discovery order
     */
    public static List<ScriptLanguage> all() {
        List<ScriptLanguage> found = new ArrayList<>();
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        ClassLoader own = ScriptLanguages.class.getClassLoader();
        if (context != null) {
            ServiceLoader.load(ScriptLanguage.class, context).forEach(found::add);
        }
        if (context != own) {
            for (ScriptLanguage lang : ServiceLoader.load(ScriptLanguage.class, own)) {
                if (found.stream().noneMatch(l -> l.getClass() == lang.getClass())) {
                    found.add(lang);
                }
            }
        }
        return found;
    }
}
