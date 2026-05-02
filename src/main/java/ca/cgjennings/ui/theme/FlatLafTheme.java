package ca.cgjennings.ui.theme;

import javax.swing.UIDefaults;

/**
 * Base class for themes that adopt one of the look-and-feels bundled with
 * FlatLaf without further customization. Concrete subclasses supply the
 * display name, group, FlatLaf L&amp;F class, and dark/light flag through
 * the constructor.
 *
 * @since 3.5
 */
abstract class FlatLafTheme extends Theme {

    private final String name;
    private final String group;
    private final String lafClassName;
    private final boolean dark;

    protected FlatLafTheme(String name, String group, String lafClassName, boolean dark) {
        this.name = name;
        this.group = group;
        this.lafClassName = lafClassName;
        this.dark = dark;
    }

    @Override
    public final String getThemeName() {
        return name;
    }

    @Override
    public final String getThemeGroup() {
        return group;
    }

    @Override
    public final String getLookAndFeelClassName() {
        return lafClassName;
    }

    @Override
    public final boolean isDark() {
        return dark;
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
    }
}
