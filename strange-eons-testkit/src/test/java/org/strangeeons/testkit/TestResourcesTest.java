package org.strangeeons.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.strangeeons.core.resources.ResourceResolver;

class TestResourcesTest {

    @TempDir
    Path tmp;

    private Path file(String rel, String content) throws IOException {
        Path p = tmp.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        return p;
    }

    private static String read(String url) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void mountsFoldersAndRestoresThePreviousResolver() throws IOException {
        file("a/resources/one.txt", "first");
        file("b/resources/one.txt", "second");
        file("b/resources/two.txt", "only b");
        ResourceResolver before = ResourceResolver.getDefault();
        try (TestResources res = TestResources.install(tmp.resolve("a/resources"), tmp.resolve("b/resources"))) {
            assertSame(res.getResolver(), ResourceResolver.getDefault());
            assertEquals("first", read("res://one.txt"));
            assertEquals("only b", read("res://two.txt"));
            assertThrows(FileNotFoundException.class, () -> read("project:x.txt"));
        }
        assertSame(before, ResourceResolver.getDefault());
    }

    @Test
    void resolvesProjectUrls() throws IOException {
        file("project/Task/card.txt", "card");
        try (TestResources res = TestResources.builder().project(tmp.resolve("project")).install()) {
            assertEquals("card", read("project:Task/card.txt"));
        }
    }

    @Test
    void searchesTheClassPathLastUnlessDisabled() throws IOException {
        file("cp/resources/cp.txt", "from loader");
        try (URLClassLoader cl = new URLClassLoader(new URL[]{tmp.resolve("cp").toUri().toURL()}, null)) {
            Thread t = Thread.currentThread();
            ClassLoader saved = t.getContextClassLoader();
            t.setContextClassLoader(cl);
            try {
                try (TestResources res = TestResources.builder().install()) {
                    assertEquals("from loader", read("res://cp.txt"));
                }
                try (TestResources res = TestResources.builder().searchClassPath(false).install()) {
                    assertNull(res.getResolver().resolve("cp.txt"));
                }
                try (TestResources res = TestResources.builder().searchClassPath(false).classLoader(cl).install()) {
                    assertEquals("from loader", read("res://cp.txt"));
                }
                try (TestResources res = TestResources.builder().searchClassPath(false)
                        .root(path -> cl.getResource("resources/" + path)).install()) {
                    assertEquals("from loader", read("res://cp.txt"));
                }
            } finally {
                t.setContextClassLoader(saved);
            }
        }
    }

    @Test
    void fallsBackToTheSystemLoaderWithoutAContextLoader() {
        Thread t = Thread.currentThread();
        ClassLoader saved = t.getContextClassLoader();
        t.setContextClassLoader(null);
        try (TestResources res = TestResources.builder().install()) {
            assertEquals(1, res.getResolver().getRoots().size());
        } finally {
            t.setContextClassLoader(saved);
        }
    }

    @Test
    void rejectsMissingArguments() {
        assertThrows(NullPointerException.class, () -> TestResources.builder().project(null));
        assertThrows(NullPointerException.class, () -> TestResources.builder().root(null));
    }
}
