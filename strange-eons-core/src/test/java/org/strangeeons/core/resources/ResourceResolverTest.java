package org.strangeeons.core.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourceResolverTest {

    @TempDir
    Path tmp;

    private ResourceResolver previous;

    @BeforeEach
    void saveDefault() {
        previous = ResourceResolver.getDefault();
    }

    @AfterEach
    void restoreDefault() {
        ResourceResolver.setDefault(previous);
    }

    private Path file(String rel, String content) throws IOException {
        Path p = tmp.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        return p;
    }

    private static String read(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void normalizesResUrls() {
        assertThat(ResourceResolver.normalize(null)).isNull();
        assertThat(ResourceResolver.normalize("icons/a.png")).isEqualTo("icons/a.png");
        assertThat(ResourceResolver.normalize("res:")).isEqualTo("");
        assertThat(ResourceResolver.normalize("res:icons/a.png")).isEqualTo("icons/a.png");
        assertThat(ResourceResolver.normalize("res://icons/a.png")).isEqualTo("icons/a.png");
        assertThat(ResourceResolver.normalize("res:///org/a.png")).isEqualTo("/org/a.png");
        assertThat(ResourceResolver.normalize("res:/org/a.png")).isEqualTo("/org/a.png");
        assertThat(ResourceResolver.normalize("res:////a.png")).isEqualTo("a.png");
        assertThat(ResourceResolver.normalize("project:a.png")).isEqualTo("project:a.png");
    }

    @Test
    void folderRootMapsRelativeAndAbsolutePaths() throws IOException {
        file("plugin/resources/icons/a.txt", "relative");
        file("plugin/org/example/b.txt", "absolute");
        ResourceRoot root = ResourceRoot.folder(tmp.resolve("plugin/resources"));
        assertThat(read(root.find("icons/a.txt"))).isEqualTo("relative");
        assertThat(read(root.find("/org/example/b.txt"))).isEqualTo("absolute");
        assertThat(root.find("missing.txt")).isNull();
    }

    @Test
    void classLoaderRootMapsRelativeAndAbsolutePaths() throws IOException {
        file("cp/resources/icons/a.txt", "relative");
        file("cp/org/example/b.txt", "absolute");
        try (URLClassLoader cl = new URLClassLoader(new URL[]{tmp.resolve("cp").toUri().toURL()}, null)) {
            ResourceRoot root = ResourceRoot.classLoader(cl);
            assertThat(read(root.find("icons/a.txt"))).isEqualTo("relative");
            assertThat(read(root.find("/org/example/b.txt"))).isEqualTo("absolute");
            assertThat(root.find("missing.txt")).isNull();
        }
    }

    @Test
    void rootsAreSearchedInOrder() throws IOException {
        file("first/resources/both.txt", "first");
        file("second/resources/both.txt", "second");
        file("second/resources/only.txt", "only second");
        ResourceResolver r = new ResourceResolver(List.of(
                ResourceRoot.folder(tmp.resolve("first/resources")),
                ResourceRoot.folder(tmp.resolve("second/resources"))));
        assertThat(read(r.resolve("both.txt"))).isEqualTo("first");
        assertThat(read(r.resolve("res://only.txt"))).isEqualTo("only second");
        assertThat(r.resolve("none.txt")).isNull();
        assertThat(r.getRoots()).hasSize(2);
        assertThat(r.getProjectFolder()).isNull();
    }

    @Test
    void absoluteUrlsAreCheckedForExistence() throws IOException {
        Path f = file("abs.txt", "here");
        ResourceResolver r = new ResourceResolver(List.of());
        assertThat(r.resolve(f.toUri().toString())).isEqualTo(f.toUri().toURL());
        assertThat(r.resolve(tmp.resolve("gone.txt").toUri().toString())).isNull();
        assertThat(r.resolve("nosuchprotocol:x")).isNull();
        assertThatThrownBy(() -> r.resolve(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resProtocolUsesTheDefaultResolver() throws IOException {
        file("res/resources/icons/a.txt", "via res");
        file("res/org/example/b.txt", "absolute via res");
        ResourceResolver.setDefault(new ResourceResolver(List.of(ResourceRoot.folder(tmp.resolve("res/resources")))));
        assertThat(ResourceResolver.getDefault().getRoots()).hasSize(1);
        assertThat(read(new URL("res://icons/a.txt"))).isEqualTo("via res");
        assertThat(read(new URL("res:///org/example/b.txt"))).isEqualTo("absolute via res");
        assertThatThrownBy(() -> read(new URL("res://icons/missing.txt"))).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void resProtocolKeepsFragments() throws IOException {
        file("frag/resources/a.txt#part", "with fragment");
        ResourceResolver.setDefault(new ResourceResolver(List.of(ResourceRoot.folder(tmp.resolve("frag/resources")))));
        assertThat(read(new URL("res://a.txt#part"))).isEqualTo("with fragment");
    }

    @Test
    void projectProtocolResolvesAgainstTheProjectFolder() throws IOException {
        file("project/Task/card.txt", "in project");
        URL folder = tmp.resolve("project").toUri().toURL();
        ResourceResolver.setDefault(new ResourceResolver(List.of(), () -> folder));
        assertThat(ResourceResolver.getDefault().getProjectFolder()).isEqualTo(folder);
        assertThat(read(new URL("project:Task/card.txt"))).isEqualTo("in project");
        assertThat(read(new URL("project://Task/card.txt"))).isEqualTo("in project");
        assertThat(read(new URL("project:///Task/card.txt"))).isEqualTo("in project");
    }

    @Test
    void projectProtocolNeedsAProject() {
        ResourceResolver.setDefault(new ResourceResolver(List.of()));
        assertThatThrownBy(() -> read(new URL("project:Task/card.txt")))
                .isInstanceOf(FileNotFoundException.class).hasMessageContaining("no open project");
    }

    @Test
    void providerIgnoresOtherProtocols() {
        assertThat(new ResourceProtocols().createURLStreamHandler("http")).isNull();
        assertThatThrownBy(() -> ResourceResolver.setDefault(null)).isInstanceOf(NullPointerException.class);
    }
}
