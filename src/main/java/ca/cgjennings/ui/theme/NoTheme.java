package ca.cgjennings.ui.theme;

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

/**
 * A theme that performs no customization. The look and feel is left at
 * whatever Swing initialized — typically the JDK's cross-platform Metal
 * L&amp;F — and {@link #applyThemeToImage} / {@link #applyThemeToColor}
 * pass their inputs through unchanged.
 *
 * <p>Selectable via the {@code theme} setting (set to this class's name)
 * or the {@code --xDisableTheme} command-line option.
 *
 * @since 3.5
 */
public class NoTheme extends Theme {

    public NoTheme() {
    }

    @Override
    public String getThemeName() {
        return "None";
    }

    @Override
    public String getLookAndFeelClassName() {
        return null;
    }

    @Override
    public LookAndFeel createLookAndFeelInstance() {
        return null;
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
    }
}
