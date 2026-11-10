package org.strangeeons.core.resources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves resource identifiers to URLs. An identifier is either a resource
 * path, a {@code res:} URL, or another absolute URL (anything with a colon).
 * Resource paths are looked up in each {@linkplain ResourceRoot root} in turn.
 *
 * <p>
 * The {@code res:} and {@code project:} URL protocols use the
 * {@linkplain #getDefault() default resolver}. Until another is
 * {@linkplain #setDefault set}, it searches the system class loader and has
 * no project.
 */
public final class ResourceResolver {

    private static final Logger log = Logger.getLogger(ResourceResolver.class.getName());

    private static volatile ResourceResolver defaultResolver = new ResourceResolver(
            List.of(ResourceRoot.classLoader(ClassLoader.getSystemClassLoader())), () -> null);

    private final List<ResourceRoot> roots;
    private final Supplier<URL> projectFolder;

    /**
     * Creates a resolver with no project.
     *
     * @param roots the roots to search, in order
     */
    public ResourceResolver(List<ResourceRoot> roots) {
        this(roots, () -> null);
    }

    /**
     * Creates a resolver.
     *
     * @param roots the roots to search, in order
     * @param projectFolder supplies the URL of the open project folder, or
     * {@code null} when no project is open; resolves {@code project:} URLs
     */
    public ResourceResolver(List<ResourceRoot> roots, Supplier<URL> projectFolder) {
        this.roots = List.copyOf(roots);
        this.projectFolder = Objects.requireNonNull(projectFolder, "projectFolder");
    }

    /**
     * Returns the resolver used by the {@code res:} and {@code project:}
     * protocols.
     *
     * @return the default resolver
     */
    public static ResourceResolver getDefault() {
        return defaultResolver;
    }

    /**
     * Sets the resolver used by the {@code res:} and {@code project:}
     * protocols.
     *
     * @param resolver the new default resolver
     */
    public static void setDefault(ResourceResolver resolver) {
        defaultResolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * Returns the roots this resolver searches.
     *
     * @return an immutable list of roots, in search order
     */
    public List<ResourceRoot> getRoots() {
        return roots;
    }

    /**
     * Returns the URL of the open project folder.
     *
     * @return the project folder URL, or {@code null} if no project is open
     */
    public URL getProjectFolder() {
        return projectFolder.get();
    }

    /**
     * Returns a URL for a resource identifier. A {@code res:} URL is first
     * reduced to its resource path. Other absolute URLs are returned if they
     * can be connected to. Resource paths are looked up in each root in turn.
     *
     * @param identifier the identifier to resolve
     * @return a URL for the resource, or {@code null} if it is not found
     */
    public URL resolve(String identifier) {
        String path = normalize(Objects.requireNonNull(identifier, "identifier"));
        if (path.indexOf(':') >= 0) {
            // cannot be a res: URL after normalization, so this cannot recurse
            try {
                URL url = new URL(path);
                url.openConnection().connect();
                return url;
            } catch (MalformedURLException e) {
                log.log(Level.WARNING, "could not create absolute URL from \"{0}\"", path);
                return null;
            } catch (IOException e) {
                return null;
            }
        }
        for (ResourceRoot root : roots) {
            URL url = root.find(path);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
     * Reduces a {@code res:} URL to the equivalent resource path. Any other
     * identifier is returned unchanged. A {@code res:} URL may have any number
     * of slashes after the colon: an odd number denotes an absolute path, so
     * one slash is kept.
     *
     * @param identifier the identifier to normalize; may be {@code null}
     * @return the equivalent resource path, or the identifier unchanged
     */
    public static String normalize(String identifier) {
        if (identifier == null || !identifier.startsWith("res:")) {
            return identifier;
        }
        int begin = 4;
        while (begin < identifier.length() && identifier.charAt(begin) == '/') {
            ++begin;
        }
        // begin starts even, so after this its parity matches the slash count
        begin -= begin & 1;
        return identifier.substring(begin);
    }
}
