package resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.strangeeons.core.Host;

class AppHostTest {

    private final AppHost host = new AppHost();

    @Test
    void delegatesToTheApplication() throws Exception {
        BufferedImage bgr = new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);
        assertThat(host.prepareImage(bgr).getType()).isEqualTo(BufferedImage.TYPE_INT_RGB);
        assertThat(host.prepareImage(null)).isNull();
        assertThat(host.getMissingVectorImage()).isSameAs(ResourceKit.getMissingVectorImage());
        assertThat(host.getMissingImage()).isSameAs(ResourceKit.getMissingImage());
        assertThat(host.resolveFontFamily("  Serif ")).isEqualTo("Serif");
        assertThat(host.resolveFontFamily(null)).isEmpty();
        assertThat(host.getGameLocale()).isNotNull();
    }

    @Test
    void vectorsNeedTheSvgLibrary() throws Exception {
        if (CoreComponents.SVG_IMAGE_SUPPORT.getInstallationState() == ca.cgjennings.apps.arkham.plugins.catalog.Catalog.VersioningState.NOT_INSTALLED) {
            assertThat(host.loadVectorImage(new URL("file:/missing.svg"))).isNull();
        }
    }

    @Test
    void installsWithTheResourceResolver() {
        Host previous = Host.get();
        try {
            ResourceKit.installCoreServices();
            assertThat(Host.get()).isInstanceOf(AppHost.class);
        } finally {
            Host.set(previous);
        }
    }

    @Test
    void localeFallsBackToTheDefault() {
        Locale loc = Language.getGameLocale();
        assertThat(host.getGameLocale()).isEqualTo(loc == null ? Locale.getDefault() : loc);
    }

    @Test
    void settingsStylesKeepApplicationColours() {
        Host previous = Host.get();
        try {
            Host.set(host);
            ca.cgjennings.layout.TextStyle style = Settings.textStyle("foreground: #ff0000; size: 12", null);
            assertThat(style.get(java.awt.font.TextAttribute.FOREGROUND)).isInstanceOf(Settings.Colour.class);
        } finally {
            Host.set(previous);
        }
    }

    @Test
    void badSettingValuesStillRaiseParseErrors() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Settings.integer("seven"))
                .isInstanceOf(Settings.ParseError.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Settings.textAlignment("sideways"))
                .isInstanceOf(Settings.ParseError.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Settings.cupShape("1,2"))
                .isInstanceOf(Settings.ParseError.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Settings.textStyle("bogus: 1", null))
                .isInstanceOf(Settings.ParseError.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Settings.number("two"))
                .isInstanceOf(Settings.ParseError.class);
        assertThat(Settings.yesNo("yes")).isTrue();
    }

    @Test
    void styleScriptValuesEvaluateWithTheRegisteredJavaScript() {
        Host previous = Host.get();
        try {
            Host.set(host);
            Object size = Settings.textStyle("size: {6 * 2}", null).get(java.awt.font.TextAttribute.SIZE);
            assertThat(((Number) size).intValue()).isEqualTo(12);
        } finally {
            Host.set(previous);
        }
    }

    @Test
    void styleScriptValuesAreCachedUnlessTheyAreScriptObjects() {
        Object first = host.evaluateStyleLiteral("'cached' + 'value'");
        assertThat(host.evaluateStyleLiteral("'cached' + 'value'")).isSameAs(first);
        Object object = host.evaluateStyleLiteral("({a: 1})");
        assertThat(object).isInstanceOf(org.mozilla.javascript.Scriptable.class);
        assertThat(host.evaluateStyleLiteral("({a: 1})")).isNotSameAs(object);
        // ScriptMonkey reports script errors to the console and yields null
        assertThat(host.evaluateStyleLiteral("throw new Error('boom')")).isNull();
    }

    @Test
    void javascriptIsARegisteredLanguage() {
        assertThat(org.strangeeons.core.scripts.ScriptLanguages.find("javascript"))
                .isInstanceOf(ca.cgjennings.apps.arkham.diy.LegacyScriptLanguage.class);
    }
}
