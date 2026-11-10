package org.strangeeons.testkit;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.strangeeons.core.resources.ResourceResolver;
import org.strangeeons.core.resources.ResourceRoot;

/**
 * Makes {@code res:} and {@code project:} URLs resolve against a plug-in's
 * source folders, as they would when the plug-in is installed. Closing it
 * restores the previous resolver.
 *
 * <pre>
 * try (TestResources res = TestResources.builder()
 *         .folder(Path.of("MyPlugin/resources"))
 *         .project(Path.of("src/test/project"))
 *         .install()) {
 *     // res://icons/logo.png now reads MyPlugin/resources/icons/logo.png
 * }
 * </pre>
 */
public final class TestResources implements AutoCloseable {

    private final ResourceResolver previous;
    private final ResourceResolver installed;

    private TestResources(ResourceResolver installed) {
        this.previous = ResourceResolver.getDefault();
        this.installed = installed;
        ResourceResolver.setDefault(installed);
    }

    /**
     * Starts describing the resources to install.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Installs resolution from the given {@code resources} folders, then the
     * test class path.
     *
     * @param resourceFolders folders searched in order
     * @return the installed resources; close to restore the previous resolver
     */
    public static TestResources install(Path... resourceFolders) {
        Builder b = builder();
        for (Path folder : resourceFolders) {
            b.folder(folder);
        }
        return b.install();
    }

    /**
     * Returns the resolver this installed.
     *
     * @return the installed resolver
     */
    public ResourceResolver getResolver() {
        return installed;
    }

    /** Restores the resolver that was the default before this was installed. */
    @Override
    public void close() {
        ResourceResolver.setDefault(previous);
    }

    /** Describes the roots and project folder to install. */
    public static final class Builder {

        private final List<ResourceRoot> roots = new ArrayList<>();
        private Path project;
        private boolean classPath = true;

        private Builder() {
        }

        /**
         * Adds a {@code resources} folder, such as the one in a plug-in's
         * source tree. Absolute resource paths resolve against its parent.
         *
         * @param resourcesFolder the folder to search
         * @return this builder
         */
        public Builder folder(Path resourcesFolder) {
            roots.add(ResourceRoot.folder(resourcesFolder));
            return this;
        }

        /**
         * Adds a class loader to search.
         *
         * @param loader the class loader
         * @return this builder
         */
        public Builder classLoader(ClassLoader loader) {
            roots.add(ResourceRoot.classLoader(loader));
            return this;
        }

        /**
         * Adds any other root.
         *
         * @param root the root to search
         * @return this builder
         */
        public Builder root(ResourceRoot root) {
            roots.add(Objects.requireNonNull(root, "root"));
            return this;
        }

        /**
         * Sets the folder that {@code project:} URLs resolve against.
         *
         * @param projectFolder the project folder
         * @return this builder
         */
        public Builder project(Path projectFolder) {
            this.project = Objects.requireNonNull(projectFolder, "projectFolder");
            return this;
        }

        /**
         * Sets whether the test class path is searched after the other roots.
         * It is by default.
         *
         * @param search whether to search the class path
         * @return this builder
         */
        public Builder searchClassPath(boolean search) {
            this.classPath = search;
            return this;
        }

        /**
         * Installs the resolver.
         *
         * @return the installed resources; close to restore the previous
         * resolver
         */
        public TestResources install() {
            List<ResourceRoot> all = new ArrayList<>(roots);
            if (classPath) {
                ClassLoader context = Thread.currentThread().getContextClassLoader();
                all.add(ResourceRoot.classLoader(context != null ? context : ClassLoader.getSystemClassLoader()));
            }
            URL projectUrl = projectUrl(project);
            return new TestResources(new ResourceResolver(all, () -> projectUrl));
        }

        private static URL projectUrl(Path project) {
            if (project == null) {
                return null;
            }
            try {
                return project.toAbsolutePath().normalize().toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("not a usable project folder: " + project, e);
            }
        }
    }
}
