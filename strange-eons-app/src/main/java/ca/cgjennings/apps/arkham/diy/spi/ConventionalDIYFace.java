package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYSheet;
import ca.cgjennings.graphics.spi.AwtImageHandle;
import java.awt.image.BufferedImage;
import java.util.Objects;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.Face;
import org.strangeeons.plugins.graphics.ImageHandle;
import org.strangeeons.plugins.graphics.Region;

/**
 * Adapter exposing a legacy {@link DIYSheet} through the
 * Conventional SPI's {@link Face} interface. The face's
 * {@link #getTemplateKey()} returns the settings key prefix for
 * this face (e.g. {@code "lore-front"}), so
 * {@link #region(String)} on key {@code "title"} resolves the
 * {@code lore-front-title-region} setting.
 *
 * <p>This adapter is the bridge between the legacy DIY framework
 * (Sheet-as-painter) and the SPI's data-only Face model. Framework
 * code constructs one adapter per sheet and caches it for the
 * lifetime of the component.
 */
public final class ConventionalDIYFace implements Face {

    private final DIY diy;
    private final DIYSheet sheet;

    public ConventionalDIYFace(DIYSheet sheet) {
        this.sheet = Objects.requireNonNull(sheet, "sheet");
        this.diy = sheet.getGameComponent();
    }

    /** Returns the wrapped legacy sheet. */
    public DIYSheet getSheet() {
        return sheet;
    }

    @Override
    public int getFaceIndex() {
        return sheet.getSheetIndex();
    }

    @Override
    public String getTemplateKey() {
        return diy.getTemplateKey(sheet.getSheetIndex());
    }

    @Override
    public int getTemplateResolution() {
        return (int) Math.round(sheet.getTemplateResolution());
    }

    @Override
    public SettingsReadWrite getSettings() {
        return diy.getSettings();
    }

    @Override
    public Region region(String key) {
        return diy.getSettings().readRegion(getTemplateKey() + "-" + key + "-region");
    }

    @Override
    public ImageHandle image(String key) {
        String fullKey = getTemplateKey() + "-" + key;
        BufferedImage image = diy.getSettings().getImageResource(fullKey);
        return image == null ? null : new AwtImageHandle(image, fullKey);
    }

    @Override
    public ImageHandle getTemplateImage() {
        BufferedImage template = sheet.getTemplateImage();
        return template == null ? null
                : new AwtImageHandle(template, getTemplateKey() + "-template");
    }
}
