/**
 * Public API namespace for Strange Eons plug-ins.
 *
 * <p>Every type in this package is a pure interface whose method signatures
 * reference only types in {@code java.*} and other types in this package.
 * Plug-ins built against these interfaces have no compile-time dependency
 * on Strange Eons internals.
 *
 * <p>Existing core types in {@code resources} and
 * {@code ca.cgjennings.apps.arkham.*} add these interfaces to their
 * {@code implements} clauses so that today's concrete types satisfy the
 * new contracts without changing their bytecode shape, return types, or
 * serialized form. Existing plug-ins compile and load unchanged.
 *
 * @since 3.5
 */
package org.strangeeons.plugins;
