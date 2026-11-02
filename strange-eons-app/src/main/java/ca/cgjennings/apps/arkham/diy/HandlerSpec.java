package ca.cgjennings.apps.arkham.diy;

/**
 * Parses the {@code path/to/script.ext:ExportedName} form of a handler
 * resource. Other forms name a class or a {@code .js} script. Only a colon
 * after the file name separates an export, since paths such as
 * {@code res://a/b.ext:Name} contain colons of their own.
 */
final class HandlerSpec {

    private HandlerSpec() {
    }

    /**
     * Returns the script's file extension with its leading dot, or {@code null}
     * if the resource does not name a script export.
     */
    static String scriptExtension(String resource) {
        int colon = resource.lastIndexOf(':');
        if (colon < 0 || colon == resource.length() - 1 || resource.indexOf('/', colon) >= 0) {
            return null;
        }
        int dot = resource.lastIndexOf('.', colon);
        if (dot < 0 || dot == colon - 1 || resource.lastIndexOf('/', colon) > dot) {
            return null;
        }
        return resource.substring(dot, colon);
    }
}
