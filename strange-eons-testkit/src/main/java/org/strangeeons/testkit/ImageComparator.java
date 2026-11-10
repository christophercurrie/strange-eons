package org.strangeeons.testkit;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Pixel-exact comparison of rendered images against masters.
 */
public final class ImageComparator {

    /**
     * Outcome for one image.
     *
     * @param path image path, relative to the compared folders, with {@code /}
     * separators
     * @param kind how the render compares with its master
     * @param detail human-readable explanation; empty for a match
     */
    public record Result(String path, Kind kind, String detail) {
    }

    /** How a render compares with its master. */
    public enum Kind {
        /** Every unmasked pixel is identical. */
        MATCH,
        /** Same size, but some unmasked pixels differ; a diff image is written. */
        DIFFERENT,
        /** The render and master have different dimensions. */
        SIZE_CHANGED,
        /** A render has no master. */
        NO_MASTER,
        /** A master has no render. */
        NOT_RENDERED
    }

    private ImageComparator() {
    }

    /**
     * Memory one comparison may need: two decoded sheets and a diff image.
     * Bounds how many run at once.
     */
    private static final long BYTES_PER_COMPARISON = 96L << 20;

    /**
     * Compares every PNG under {@code actual} with the same relative path
     * under {@code masters}, several at a time. Writes a diff image for each
     * mismatch to {@code diffs}. Results are in path order.
     *
     * @param actual folder of rendered images
     * @param masters folder of master images; need not exist
     * @param diffs folder that receives diff images
     * @return one result per image path found in either folder
     * @throws IOException if an image cannot be read or a diff written
     */
    public static List<Result> compare(Path actual, Path masters, Path diffs) throws IOException {
        return compare(actual, masters, diffs, MasterPlan.SAME_PATH);
    }

    /**
     * Compares every PNG under {@code actual} with the master the plan gives
     * it. A master that no render has at its own path was not rendered.
     *
     * @param actual folder of rendered images
     * @param masters folder of master images; need not exist
     * @param diffs folder that receives diff images
     * @param plan which master each render is checked against
     * @return one result per image path found in either folder
     * @throws IOException if an image cannot be read or a diff written
     */
    public static List<Result> compare(Path actual, Path masters, Path diffs, MasterPlan plan) throws IOException {
        TreeSet<String> names = new TreeSet<>(pngs(actual));
        names.addAll(pngs(masters));
        ForkJoinPool pool = new ForkJoinPool(parallelism(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory()));
        try {
            return pool.submit(() -> names.parallelStream().map(n -> {
                try {
                    return compare(n, actual, masters, diffs, plan);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toList()).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UncheckedIOException u) {
                throw u.getCause();
            }
            throw new IOException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } finally {
            pool.shutdown();
        }
    }

    /** Comparisons to run at once: one per processor, as memory allows. */
    static int parallelism(int processors, long maxMemory) {
        return (int) Math.max(1, Math.min(processors, maxMemory / BYTES_PER_COMPARISON));
    }

    private static Result compare(String n, Path actual, Path masters, Path diffs, MasterPlan plan)
            throws IOException {
        Path a = actual.resolve(n);
        if (!Files.exists(a)) {
            return new Result(n, Kind.NOT_RENDERED, "master exists but nothing was rendered");
        }
        String master = plan.masterFor(n);
        Path m = masters.resolve(master);
        if (!Files.exists(m)) {
            return new Result(n, Kind.NO_MASTER, master.equals(n) ? "no master image" : "no master image " + master);
        }
        return compareImages(n, read(a), read(m), plan.maskFor(n), diffs.resolve(n));
    }

    /**
     * Compares a rendered image with a master image file. Writes a diff image
     * to {@code diff} if they differ.
     *
     * @param actual the rendered image
     * @param master the master image file; need not exist
     * @param diff the file that receives a diff image if they differ
     * @return the result, whose path is the master's file name
     * @throws IOException if the master cannot be read or the diff written
     */
    public static Result compare(BufferedImage actual, Path master, Path diff) throws IOException {
        String n = master.getFileName().toString();
        if (!Files.exists(master)) {
            return new Result(n, Kind.NO_MASTER, "no master image");
        }
        return compareImages(n, actual, read(master), null, diff);
    }

    private static Result compareImages(String n, BufferedImage ai, BufferedImage mi, Rectangle mask, Path d)
            throws IOException {
        int w = ai.getWidth();
        int h = ai.getHeight();
        if (w != mi.getWidth() || h != mi.getHeight()) {
            return new Result(n, Kind.SIZE_CHANGED, String.format("size %dx%d, master %dx%d",
                    w, h, mi.getWidth(), mi.getHeight()));
        }
        int[] ar = new int[w];
        int[] mr = new int[w];
        long differing = 0;
        for (int y = 0; y < h; ++y) {
            ai.getRGB(0, y, w, 1, ar, 0, w);
            mi.getRGB(0, y, w, 1, mr, 0, w);
            for (int x = 0; x < w; ++x) {
                if (ar[x] != mr[x] && !masked(mask, x, y)) {
                    ++differing;
                }
            }
        }
        if (differing == 0) {
            return new Result(n, Kind.MATCH, "");
        }
        BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; ++y) {
            ai.getRGB(0, y, w, 1, ar, 0, w);
            mi.getRGB(0, y, w, 1, mr, 0, w);
            for (int x = 0; x < w; ++x) {
                ar[x] = ar[x] != mr[x] && !masked(mask, x, y) ? 0xffff00ff : faded(ar[x]);
            }
            diff.setRGB(0, y, w, 1, ar, 0, w);
        }
        Files.createDirectories(d.toAbsolutePath().getParent());
        ImageIO.write(diff, "png", d.toFile());
        double pct = 100.0 * differing / ((double) w * h);
        return new Result(n, Kind.DIFFERENT, String.format("%d pixels differ (%.3f%%)", differing, pct));
    }

    private static boolean masked(Rectangle mask, int x, int y) {
        return mask != null && mask.contains(x, y);
    }

    /** Greyed, translucent copy of a pixel so magenta differences stand out. */
    private static int faded(int argb) {
        int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
        int grey = (r * 30 + g * 59 + b * 11) / 100;
        grey = 128 + grey / 2;
        return 0x60000000 | (grey << 16) | (grey << 8) | grey;
    }

    private static BufferedImage read(Path p) throws IOException {
        BufferedImage i = ImageIO.read(p.toFile());
        if (i == null) {
            throw new IOException("not a readable image: " + p);
        }
        return i;
    }

    private static List<String> pngs(Path root) throws IOException {
        if (root == null || !Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .map(p -> root.relativize(p).toString().replace('\\', '/'))
                    .toList();
        }
    }
}
