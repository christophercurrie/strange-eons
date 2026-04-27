# Changelog

## 2026-04-26

- Fixed a memory leak in `AbstractGameComponentEditor`: the `pcl`
  `PropertyChangeListener` was registered against
  `StrangeEonsAppWindow.VIEW_BACKDROP_PROPERTY` in the constructor but
  `dispose()` tried to remove it under `VIEW_QUALITY_PROPERTY`. Because the
  property names did not match, the listener was never actually removed and
  every editor ever opened stayed pinned in the long-lived `AppFrame` listener
  list, transitively retaining the editor's sheets, viewers, and game
  component (including portrait images). This explains the long-reported
  pattern of Strange Eons growing slower with each `.eons` file opened.
- Added JUnit 5 (5.10.2) and Mockito (5.18.0) as test dependencies, plus the
  Maven Surefire plugin (3.2.5). Mockito needs
  `-Dnet.bytebuddy.experimental=true` because Byte Buddy 1.17.x does not yet
  officially support JDK 25 bytecode.
- Added `AbstractGameComponentEditorTest`, a regression test that verifies
  the property name passed to `addPropertyChangeListener` in the constructor
  matches the one passed to `removePropertyChangeListener` in `dispose()`.
  Note: the test cannot run under `java.awt.headless=true` because
  `PlatformSupport.<clinit>` calls `Toolkit.getMenuShortcutKeyMask()`, which
  throws `HeadlessException`. Worth revisiting if we want CI to run tests
  headless.
