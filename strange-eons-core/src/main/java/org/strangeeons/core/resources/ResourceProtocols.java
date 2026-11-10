package org.strangeeons.core.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

/**
 * Provides the {@code res:} and {@code project:} URL protocols, which resolve
 * through the {@linkplain ResourceResolver#getDefault() default resolver}.
 * Registered as a service, so the protocols are available on any class path
 * that includes this module.
 */
public final class ResourceProtocols extends URLStreamHandlerProvider {

    /** Creates the provider; called by the service loader. */
    public ResourceProtocols() {
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        switch (protocol) {
            case "res":
                return new ResHandler();
            case "project":
                return new ProjectHandler();
            default:
                return null;
        }
    }

    /**
     * Joins the host and path of a URL; resource URLs are file system-like and
     * do not use a host.
     */
    static String composedPath(URL url) {
        String host = url.getHost();
        return host == null || host.isEmpty() ? url.getPath() : host + url.getPath();
    }

    static final class ResHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            String path = composedPath(url);
            if (url.getRef() != null) {
                path += '#' + url.getRef();
            }
            URL resolved = ResourceResolver.getDefault().resolve(path);
            if (resolved == null) {
                throw new FileNotFoundException(url.toExternalForm());
            }
            return resolved.openConnection();
        }
    }

    static final class ProjectHandler extends URLStreamHandler {
        @Override
        protected void parseURL(URL u, String spec, int start, int limit) {
            super.parseURL(u, spec, start, limit);
            if (u.getHost() != null && !u.getHost().isEmpty()) {
                setURL(u, u.getProtocol(), "", -1, null, null, u.getHost() + u.getPath(), u.getQuery(), u.getRef());
            }
        }

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            String path = composedPath(url);
            int slashes = 0;
            while (slashes < path.length() && path.charAt(slashes) == '/') {
                ++slashes;
            }
            path = path.substring(slashes);
            URL project = ResourceResolver.getDefault().getProjectFolder();
            if (project == null) {
                throw new FileNotFoundException(url.toExternalForm() + " (no open project)");
            }
            return new URL(project, path).openConnection();
        }
    }
}
