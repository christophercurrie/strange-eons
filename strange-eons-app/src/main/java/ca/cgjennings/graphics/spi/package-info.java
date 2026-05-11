/**
 * AWT-backed implementations of the
 * {@link org.strangeeons.plugins.graphics} SPI. {@link AwtGraphics}
 * adapts a {@link java.awt.Graphics2D} into the toolkit-neutral
 * {@link org.strangeeons.plugins.graphics.Graphics} surface;
 * {@link AwtImageHandle} wraps a {@link java.awt.image.BufferedImage}
 * as an {@link org.strangeeons.plugins.graphics.ImageHandle}.
 *
 * <p>Plug-ins never see these types: the framework hands plug-ins
 * the SPI interface, the implementations live here.
 */
package ca.cgjennings.graphics.spi;
