package org.strangeeons.plugins;

/**
 * Top-level interface for a Strange Eons plug-in built against the
 * {@code org.strangeeons.plugins} API namespace. A pure interface whose
 * method signatures reference only types in {@code java.*} and the rest
 * of this package.
 *
 * <p>This interface mirrors a subset of the legacy
 * {@code ca.cgjennings.apps.arkham.plugins.Plugin}. The icon and
 * representative-image methods are intentionally omitted; they will be
 * added in a follow-up step that resolves how UI types cross the API
 * boundary.
 *
 * <p>Existing plug-ins continue to implement
 * {@code ca.cgjennings.apps.arkham.plugins.Plugin} and require no changes.
 * Plug-ins targeting this namespace are loaded via an adapter (see the
 * Tier 2 work-in-progress) so the application's internal call sites
 * continue to operate on the legacy {@code Plugin} type.
 *
 * @since 3.5
 */
public interface Plugin {

    /** Type value for an activated plug-in (a Toolbox-menu command). */
    int ACTIVATED = 0;
    /** Type value for an injected plug-in (loaded on demand, no menu item). */
    int INJECTED = 1;
    /** Type value for an extension plug-in (loaded once at startup). */
    int EXTENSION = 2;

    /**
     * Called once before any other lifecycle method to give the plug-in a
     * chance to initialize. Returning {@code false} aborts the plug-in's
     * registration without raising an error dialog.
     */
    boolean initializePlugin(PluginContext context);

    /**
     * Called when the plug-in is being unloaded. The plug-in should release
     * any resources it has acquired.
     */
    void unloadPlugin();

    /** Returns a short, ideally localized name for the plug-in. */
    String getPluginName();

    /** Returns a one-sentence description of the plug-in. */
    String getPluginDescription();

    /** Returns the plug-in's internal version number. */
    float getPluginVersion();

    /**
     * Returns the plug-in's type — one of {@link #ACTIVATED},
     * {@link #INJECTED}, or {@link #EXTENSION}.
     */
    int getPluginType();

    /**
     * Activates or deactivates an {@code ACTIVATED} plug-in. Called when
     * the user invokes the plug-in's menu item.
     */
    void showPlugin(PluginContext context, boolean show);

    /**
     * Returns whether this plug-in's interface is currently showing or
     * running. Only meaningful for {@code ACTIVATED} plug-ins.
     */
    boolean isPluginShowing();

    /**
     * Returns whether the plug-in is currently in a state where it can
     * usefully be activated.
     */
    boolean isPluginUsable();

    /**
     * Returns a string describing the preferred default accelerator key
     * (e.g. {@code "ctrl Q"}), or {@code null} for no default.
     */
    String getDefaultAcceleratorKey();
}
