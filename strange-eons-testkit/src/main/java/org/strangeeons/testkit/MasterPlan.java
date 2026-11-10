package org.strangeeons.testkit;

import java.awt.Rectangle;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;

/**
 * Which master each render is checked against. By default a render's master
 * has the same path. A render ending in {@code renderSuffix}, unless
 * excluded, shares the master of the render ending in {@code masterSuffix}
 * instead, such as a back that repeats its front; the first matching mask
 * hides the region where they may differ, in both images.
 */
public final class MasterPlan {

    /** Checks every render against the master at its own path. */
    public static final MasterPlan SAME_PATH = new MasterPlan(null, null, List.of(), Map.of());

    private final String renderSuffix;
    private final String masterSuffix;
    private final List<PathMatcher> excludes;
    private final List<Map.Entry<PathMatcher, Rectangle>> masks;

    /**
     * Creates a plan.
     *
     * @param renderSuffix suffix of renders that share another master, or null
     * @param masterSuffix suffix that replaces it to name the master, or null
     * @param excludes globs of render paths that keep their own masters
     * @param masks regions left out of shared comparisons, by render path glob;
     * the first match in iteration order applies
     * @throws IllegalArgumentException if only one suffix is given
     */
    public MasterPlan(String renderSuffix, String masterSuffix, List<String> excludes, Map<String, Rectangle> masks) {
        if ((renderSuffix == null) != (masterSuffix == null)) {
            throw new IllegalArgumentException("Set both renderSuffix and masterSuffix, or neither");
        }
        this.renderSuffix = renderSuffix;
        this.masterSuffix = masterSuffix;
        this.excludes = excludes.stream().map(MasterPlan::glob).toList();
        this.masks = masks.entrySet().stream().map(e -> Map.entry(glob(e.getKey()), e.getValue())).toList();
    }

    /**
     * Whether the render shares another render's master.
     *
     * @param render render path
     * @return true if checked against another render's master
     */
    public boolean isShared(String render) {
        return renderSuffix != null && render.endsWith(renderSuffix)
                && excludes.stream().noneMatch(m -> m.matches(Path.of(render)));
    }

    /**
     * Path of the master the render is checked against.
     *
     * @param render render path
     * @return master path
     */
    public String masterFor(String render) {
        return isShared(render)
                ? render.substring(0, render.length() - renderSuffix.length()) + masterSuffix
                : render;
    }

    /**
     * Region left out of the comparison.
     *
     * @param render render path
     * @return the masked region, or null
     */
    public Rectangle maskFor(String render) {
        if (!isShared(render)) {
            return null;
        }
        return masks.stream().filter(e -> e.getKey().matches(Path.of(render))).map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static PathMatcher glob(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    }
}
