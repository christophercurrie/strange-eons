/**
 * Adapters that surface the legacy DIY framework through the
 * Conventional component SPI ({@code
 * org.strangeeons.plugins.conventional}). The
 * {@link ComponentHandlerBridge} adapts a SPI
 * {@code ComponentHandler} onto the legacy
 * {@link ca.cgjennings.apps.arkham.diy.Handler} interface; each
 * {@link ca.cgjennings.apps.arkham.diy.DIYSheet} face is wrapped in a
 * {@link ConventionalDIYFace} so the handler receives an SPI-shaped
 * {@code Face} during painter and paint dispatch.
 *
 * <p>Plug-ins do not construct these adapters directly; the
 * framework manages their lifecycle.
 */
package ca.cgjennings.apps.arkham.diy.spi;
