package org.strangeeons.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageComparatorTest {

    @TempDir
    Path tmp;

    private void png(String rel, int w, int h, int rgb) throws IOException {
        BufferedImage i = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                i.setRGB(x, y, rgb);
            }
        }
        Path p = tmp.resolve(rel);
        Files.createDirectories(p.getParent());
        ImageIO.write(i, "png", p.toFile());
    }

    @Test
    void classifiesEveryCase() throws IOException {
        png("actual/same.png", 4, 4, 0xff112233);
        png("masters/same.png", 4, 4, 0xff112233);
        png("actual/sub/changed.png", 4, 4, 0xff112233);
        png("masters/sub/changed.png", 4, 4, 0xff112234);
        png("actual/resized.png", 4, 5, 0xff000000);
        png("masters/resized.png", 4, 4, 0xff000000);
        png("actual/new.png", 2, 2, 0xff000000);
        png("masters/gone.png", 2, 2, 0xff000000);

        List<ImageComparator.Result> r = ImageComparator.compare(tmp.resolve("actual"), tmp.resolve("masters"), tmp.resolve("diffs"));
        Map<String, ImageComparator.Kind> kinds = r.stream().collect(Collectors.toMap(ImageComparator.Result::path, ImageComparator.Result::kind));

        assertEquals(Map.of(
                "same.png", ImageComparator.Kind.MATCH,
                "sub/changed.png", ImageComparator.Kind.DIFFERENT,
                "resized.png", ImageComparator.Kind.SIZE_CHANGED,
                "new.png", ImageComparator.Kind.NO_MASTER,
                "gone.png", ImageComparator.Kind.NOT_RENDERED), kinds);
        assertTrue(Files.exists(tmp.resolve("diffs/sub/changed.png")));
    }

    @Test
    void missingMastersFolderMeansNoMasters() throws IOException {
        png("actual/a.png", 1, 1, 0xff000000);
        List<ImageComparator.Result> r = ImageComparator.compare(tmp.resolve("actual"), tmp.resolve("nope"), tmp.resolve("diffs"));
        assertEquals(ImageComparator.Kind.NO_MASTER, r.get(0).kind());
    }

    @Test
    void diffMarksOnlyDifferingPixels() throws IOException {
        png("actual/a.png", 3, 2, 0xff000000);
        png("masters/a.png", 3, 2, 0xff000000);
        BufferedImage changed = ImageIO.read(tmp.resolve("actual/a.png").toFile());
        changed.setRGB(1, 1, 0xffffffff);
        ImageIO.write(changed, "png", tmp.resolve("actual/a.png").toFile());

        ImageComparator.Result r = ImageComparator.compare(tmp.resolve("actual"), tmp.resolve("masters"),
                tmp.resolve("diffs")).get(0);
        assertEquals("1 pixels differ (16.667%)", r.detail());
        BufferedImage diff = ImageIO.read(tmp.resolve("diffs/a.png").toFile());
        assertEquals(0xffff00ff, diff.getRGB(1, 1));
        assertEquals(0x60808080, diff.getRGB(0, 0));
    }

    @Test
    void resultsAreInPathOrder() throws IOException {
        for (int i = 0; i < 40; ++i) {
            png(String.format("actual/%02d.png", i), 8, 8, 0xff000000 | i);
            png(String.format("masters/%02d.png", i), 8, 8, 0xff000000 | i);
        }
        List<String> paths = ImageComparator.compare(tmp.resolve("actual"), tmp.resolve("masters"),
                tmp.resolve("diffs")).stream().map(ImageComparator.Result::path).toList();
        assertEquals(paths.stream().sorted().toList(), paths);
        assertEquals(40, paths.size());
    }

    @Test
    void unreadableImageFails() throws IOException {
        png("masters/bad.png", 1, 1, 0xff000000);
        Files.createDirectories(tmp.resolve("actual"));
        Files.writeString(tmp.resolve("actual/bad.png"), "not a png");
        IOException e = assertThrows(IOException.class, () -> ImageComparator.compare(tmp.resolve("actual"),
                tmp.resolve("masters"), tmp.resolve("diffs")));
        assertTrue(e.getMessage().contains("not a readable image"), e.getMessage());
    }

    @Test
    void parallelismIsBoundedByProcessorsAndMemory() {
        assertEquals(8, ImageComparator.parallelism(8, 4L << 30));
        assertEquals(5, ImageComparator.parallelism(8, 512L << 20));
        assertEquals(1, ImageComparator.parallelism(8, 16L << 20));
        assertEquals(1, ImageComparator.parallelism(1, 4L << 30));
    }

    @Test
    void sharedMasterIsComparedOutsideItsMask() throws IOException {
        png("actual/c/a-0.png", 4, 4, 0xff000000);
        png("masters/c/a-0.png", 4, 4, 0xff000000);
        png("actual/c/a-1.png", 4, 4, 0xff000000);
        BufferedImage back = ImageIO.read(tmp.resolve("actual/c/a-1.png").toFile());
        back.setRGB(0, 0, 0xffffffff);
        ImageIO.write(back, "png", tmp.resolve("actual/c/a-1.png").toFile());
        png("actual/c/b-1.png", 4, 4, 0xff000000);

        MasterPlan plan = new MasterPlan("-1.png", "-0.png", List.of(),
                Map.of("c/a-*", new java.awt.Rectangle(0, 0, 1, 1)));
        Map<String, ImageComparator.Result> r = ImageComparator.compare(tmp.resolve("actual"), tmp.resolve("masters"),
                tmp.resolve("diffs"), plan).stream()
                .collect(Collectors.toMap(ImageComparator.Result::path, x -> x));
        assertEquals(ImageComparator.Kind.MATCH, r.get("c/a-1.png").kind());
        assertEquals(ImageComparator.Kind.NO_MASTER, r.get("c/b-1.png").kind());
        assertEquals("no master image c/b-0.png", r.get("c/b-1.png").detail());

        // unmasked, the same pixel differs
        MasterPlan unmasked = new MasterPlan("-1.png", "-0.png", List.of(), Map.of());
        assertEquals(ImageComparator.Kind.DIFFERENT, ImageComparator.compare(tmp.resolve("actual"),
                tmp.resolve("masters"), tmp.resolve("diffs"), unmasked).stream()
                .filter(x -> x.path().equals("c/a-1.png")).findFirst().orElseThrow().kind());
    }

    @Test
    void comparesASingleImageWithItsMaster() throws IOException {
        png("masters/one.png", 3, 3, 0xff102030);
        BufferedImage same = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                same.setRGB(x, y, 0xff102030);
            }
        }
        Path master = tmp.resolve("masters/one.png");
        Path diff = tmp.resolve("out/diffs/one.png");
        assertEquals(ImageComparator.Kind.MATCH, ImageComparator.compare(same, master, diff).kind());
        assertTrue(Files.notExists(diff));

        same.setRGB(1, 1, 0xffffffff);
        ImageComparator.Result r = ImageComparator.compare(same, master, diff);
        assertEquals(ImageComparator.Kind.DIFFERENT, r.kind());
        assertEquals("one.png", r.path());
        assertTrue(Files.exists(diff));

        assertEquals(ImageComparator.Kind.SIZE_CHANGED, ImageComparator.compare(
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), master, diff).kind());
        assertEquals(ImageComparator.Kind.NO_MASTER, ImageComparator.compare(
                same, tmp.resolve("masters/none.png"), diff).kind());
    }
}
