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
}
