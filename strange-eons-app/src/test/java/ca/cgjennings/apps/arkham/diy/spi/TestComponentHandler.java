package ca.cgjennings.apps.arkham.diy.spi;

import org.strangeeons.plugins.conventional.ComponentHandler;

/**
 * Minimal {@link ComponentHandler} used by
 * {@link ComponentHandlerLoadIntegrationTest} to exercise the
 * class-name load path. The class is public with a no-arg
 * constructor because {@code BundleInstaller.findClassAcrossBundles}
 * instantiates the handler reflectively. Every method picks up
 * {@code ComponentHandler}'s no-op defaults.
 */
public class TestComponentHandler implements ComponentHandler {
}
