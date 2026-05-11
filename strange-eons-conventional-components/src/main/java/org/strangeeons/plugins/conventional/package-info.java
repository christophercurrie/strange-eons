/**
 * The Conventional component framework — an opinionated layer on
 * top of the bare {@code GameComponent} SPI for face-bearing
 * components (cards, markers, tokens). The framework prescribes
 * face/sheet/editor/portrait conventions; the plug-in author
 * supplies a {@link ComponentHandler} that fills in painting and
 * control wiring.
 *
 * <p>Three-layer plug-in story:
 *
 * <ul>
 *   <li><b>{@code GameComponent}</b> — bare; the plug-in author
 *       implements everything from scratch. Appropriate for
 *       components that don't fit a face-bearing model.</li>
 *   <li><b>Conventional</b> (this framework) — opinionated lifecycle
 *       and editor scaffolding; the plug-in author writes a
 *       {@link ComponentHandler}.</li>
 *   <li><b>Prefab</b> — settings-driven specialization on top of
 *       Conventional; the plug-in author supplies a base key and a
 *       {@code .settings} file.</li>
 * </ul>
 *
 * <p>Types in this package reference only {@code java.*} and
 * {@code org.strangeeons.plugins.*}. Plug-ins implementing
 * {@link ComponentHandler} can compile against
 * {@code strange-eons-plugin-api} and
 * {@code strange-eons-conventional-components} without depending on
 * the application module.
 *
 * @since 3.6
 */
package org.strangeeons.plugins.conventional;
