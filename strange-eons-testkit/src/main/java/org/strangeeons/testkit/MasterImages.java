package org.strangeeons.testkit;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.imageio.ImageIO;

/**
 * Checks rendered images against master images stored with a plug-in's tests.
 * Each check saves the rendered image to the output folder, so a failed check
 * leaves the render and a diff image next to each other.
 *
 * <p>
 * Running with the system property {@value #UPDATE_PROPERTY} set to
 * {@code true} records each rendered image as its new master instead.
 *
 * <pre>
 * MasterImages masters = new MasterImages(Path.of("src/test/masters"), Path.of("build/masters"));
 * masters.verify("title", MarkupImages.render("&lt;b&gt;Title", 200, 40, 300));
 * </pre>
 */
public final class MasterImages {

    /** System property that, when {@code true}, records new masters. */
    public static final String UPDATE_PROPERTY = "strangeeons.updateMasters";

    private final Path masters;
    private final Path output;

    /**
     * Creates a checker.
     *
     * @param masters the folder of master images
     * @param output the folder that receives renders and diffs
     */
    public MasterImages(Path masters, Path output) {
        this.masters = Objects.requireNonNull(masters, "masters");
        this.output = Objects.requireNonNull(output, "output");
    }

    /**
     * Returns whether checks record new masters rather than compare.
     *
     * @return true if {@value #UPDATE_PROPERTY} is {@code true}
     */
    public static boolean isUpdating() {
        return Boolean.getBoolean(UPDATE_PROPERTY);
    }

    /**
     * Saves a rendered image and compares it with its master, or records it as
     * the master when {@linkplain #isUpdating() updating}.
     *
     * @param name the image name, without extension; may include folders
     * @param actual the rendered image
     * @return the comparison result; a match when updating
     * @throws IOException if an image cannot be read or written
     */
    public ImageComparator.Result check(String name, BufferedImage actual) throws IOException {
        String file = name + ".png";
        write(actual, output.resolve("actual").resolve(file));
        Path master = masters.resolve(file);
        if (isUpdating()) {
            write(actual, master);
            return new ImageComparator.Result(file, ImageComparator.Kind.MATCH, "master updated");
        }
        ImageComparator.Result r = ImageComparator.compare(actual, master, output.resolve("diffs").resolve(file));
        return new ImageComparator.Result(file, r.kind(), r.detail());
    }

    /**
     * Checks a rendered image and fails if it does not match its master.
     *
     * @param name the image name, without extension; may include folders
     * @param actual the rendered image
     * @throws IOException if an image cannot be read or written
     * @throws AssertionError if the image does not match its master
     */
    public void verify(String name, BufferedImage actual) throws IOException {
        ImageComparator.Result r = check(name, actual);
        if (r.kind() != ImageComparator.Kind.MATCH) {
            throw new AssertionError(r.path() + ": " + r.kind() + ' ' + r.detail()
                    + " (set -D" + UPDATE_PROPERTY + "=true to record masters)");
        }
    }

    private static void write(BufferedImage image, Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        ImageIO.write(image, "png", file.toFile());
    }
}
