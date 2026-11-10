package org.strangeeons.testkit;

import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.layout.TextStyle;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import javax.imageio.ImageIO;
import org.strangeeons.core.Host;
import org.strangeeons.core.graphics.awt.AwtImageHandle;
import org.strangeeons.core.resources.ResourceResolver;
import org.strangeeons.core.settings.SettingValues;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.SettingsReader;
import org.strangeeons.plugins.graphics.ImageHandle;
import org.strangeeons.plugins.graphics.PageShape;

/**
 * Settings for testing code written against the plug-in SPI without the
 * application. Values are parsed as the application parses them. A key that is
 * not set here is looked up in the parent, if any; setting a key to
 * {@code null} hides the parent's value, while {@linkplain #reset resetting}
 * it shows the parent's value again.
 *
 * <pre>
 * TestSettings settings = new TestSettings();
 * settings.addSettingsFrom("MyGame/card.settings");
 * handler.paint(face, graphics, settings);
 * </pre>
 */
public final class TestSettings implements SettingsReadWrite {

    private final Map<String, String> values = new HashMap<>();
    private final SettingsReader parent;

    /** Creates empty settings with no parent. */
    public TestSettings() {
        this(null);
    }

    /**
     * Creates empty settings that fall back on a parent.
     *
     * @param parent the settings to consult for keys not set here; may be
     * {@code null}
     */
    public TestSettings(SettingsReader parent) {
        this.parent = parent;
    }

    /**
     * Creates settings with the given values.
     *
     * @param values the keys and values to set
     * @return new settings with no parent
     */
    public static TestSettings of(Map<String, String> values) {
        TestSettings s = new TestSettings();
        values.forEach(s::set);
        return s;
    }

    @Override
    public String get(String key) {
        Objects.requireNonNull(key, "key");
        if (values.containsKey(key)) {
            return values.get(key);
        }
        return parent == null ? null : parent.get(key);
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public String getText(String key) {
        String value = get(suffix(key, "-text"));
        return value == null ? "[MISSING: " + key + "]" : value;
    }

    @Override
    public boolean getYesNo(String key) {
        return SettingValues.yesNo(get(key));
    }

    @Override
    public int getInt(String key) {
        return parse(key, SettingValues::integer);
    }

    @Override
    public float getFloat(String key) {
        return parse(key, SettingValues::number).floatValue();
    }

    @Override
    public double getDouble(String key) {
        return parse(key, SettingValues::number);
    }

    @Override
    public Set<String> getKeySet() {
        return Collections.unmodifiableSet(new HashSet<>(values.keySet()));
    }

    @Override
    public Set<String> getVisibleKeySet() {
        Set<String> keys = new HashSet<>(values.keySet());
        if (parent != null) {
            keys.addAll(parent.getVisibleKeySet());
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public SettingsReader getParent() {
        return parent;
    }

    @Override
    public org.strangeeons.plugins.graphics.TextStyle readTextStyle(String key,
            org.strangeeons.plugins.graphics.TextStyle base) {
        String value = get(suffix(key, "-style"));
        if (value == null) {
            return base;
        }
        if (base != null && !(base instanceof TextStyle)) {
            throw new IllegalArgumentException("unsupported TextStyle implementation: " + base.getClass().getName());
        }
        return parse(key, value, v -> SettingValues.textStyle(v, (TextStyle) base));
    }

    @Override
    public PageShape readPageShape(String key) {
        String value = get(suffix(key, "-shape"));
        return value == null ? null : parse(key, value, SettingValues::cupShape);
    }

    @Override
    public int readAlignment(String key) {
        String value = get(suffix(key, "-alignment"));
        return value == null ? MarkupRenderer.LAYOUT_LEFT | MarkupRenderer.LAYOUT_TOP
                : parse(key, value, SettingValues::textAlignment);
    }

    /**
     * Reads the image named by a key's value, resolved like other resources.
     * An image that cannot be read is replaced by the host's stand-in image.
     */
    @Override
    public ImageHandle readImage(String key) {
        String value = get(key);
        return value == null ? null : new AwtImageHandle(image(value), key);
    }

    @Override
    public void set(String key, String value) {
        values.put(Objects.requireNonNull(key, "key"), value);
    }

    @Override
    public void setText(String key, String value) {
        set(suffix(key, "-text"), value);
    }

    @Override
    public void setYesNo(String key, boolean value) {
        set(key, value ? "yes" : "no");
    }

    @Override
    public void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    @Override
    public void setFloat(String key, float value) {
        set(key, String.valueOf(value));
    }

    @Override
    public void setDouble(String key, double value) {
        set(key, String.valueOf(value));
    }

    @Override
    public void reset(String key) {
        values.remove(Objects.requireNonNull(key, "key"));
    }

    /**
     * Adds the settings in a {@code .settings} or {@code .properties}
     * resource, resolved through the {@linkplain ResourceResolver#getDefault()
     * default resolver}.
     *
     * @param resource the resource identifier
     * @throws FileNotFoundException if the resource cannot be found
     * @throws IOException if the resource cannot be read
     */
    @Override
    public void addSettingsFrom(String resource) throws IOException {
        Objects.requireNonNull(resource, "resource");
        if (resource.isEmpty()) {
            return;
        }
        URL url = ResourceResolver.getDefault().resolve(resource);
        if (url == null) {
            throw new FileNotFoundException(resource);
        }
        Properties loaded = new Properties();
        try (InputStream in = url.openStream()) {
            loaded.load(in);
        }
        for (String key : loaded.stringPropertyNames()) {
            set(key, loaded.getProperty(key));
        }
    }

    @Override
    public String toString() {
        return "TestSettings" + values;
    }

    private <T> T parse(String key, Function<String, T> parser) {
        return parse(key, get(key), parser);
    }

    private static <T> T parse(String key, String value, Function<String, T> parser) {
        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("unable to parse " + key + " = " + value, e);
        }
    }

    private static String suffix(String key, String suffix) {
        return key.endsWith(suffix) ? key : key + suffix;
    }

    private static BufferedImage image(String identifier) {
        URL url = ResourceResolver.getDefault().resolve(identifier);
        if (url != null) {
            try {
                BufferedImage im = ImageIO.read(url);
                if (im != null) {
                    return Host.get().prepareImage(im);
                }
            } catch (IOException e) {
                // fall through to the stand-in
            }
        }
        return Host.get().getMissingImage();
    }
}
