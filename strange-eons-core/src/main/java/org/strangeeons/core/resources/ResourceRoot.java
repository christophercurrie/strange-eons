package org.strangeeons.core.resources;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A place where a {@link ResourceResolver} looks for resources. A root is
 * given a normalized resource path: either relative to the virtual
 * {@code resources} folder ({@code icons/logo.png}), or absolute from the
 * class path root ({@code /org/example/logo.png}).
 */
@FunctionalInterface
public interface ResourceRoot {

    /**
     * Returns a URL for the resource, or {@code null} if this root does not
     * have it.
     *
     * @param path a normalized resource path
     * @return the resource URL, or {@code null}
     */
    URL find(String path);

    /**
     * Returns a root that looks up resources through a class loader. Relative
     * paths are looked up under {@code resources/}; absolute paths from the
     * class path root.
     *
     * @param loader the class loader to search
     * @return a class loader root
     */
    static ResourceRoot classLoader(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader");
        return path -> loader.getResource(path.startsWith("/") ? path.substring(1) : "resources/" + path);
    }

    /**
     * Returns a root that reads files from a {@code resources} folder, such as
     * the one in a plug-in's source tree. Relative paths are looked up in the
     * folder; absolute paths in its parent, which plays the role of the class
     * path root.
     *
     * @param resourcesFolder the folder that holds relative resources
     * @return a folder root
     */
    static ResourceRoot folder(Path resourcesFolder) {
        Objects.requireNonNull(resourcesFolder, "resourcesFolder");
        Path base = resourcesFolder.toAbsolutePath().normalize();
        return path -> {
            String relative = path.startsWith("/") ? ".." + path : path;
            Path file = base.resolve(relative.replace('/', File.separatorChar)).normalize();
            if (!Files.exists(file)) {
                return null;
            }
            try {
                return file.toUri().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        };
    }
}
