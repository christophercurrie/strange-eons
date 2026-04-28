# Changelog

## 2026-04-27

- Released sheet rasters for inactive game-component editors to cut
  working-set memory. With many editors open, `BufferedImage` pixel
  buffers held by background editors' sheets dominated heap retention
  (per profiling on `chore/memory-profiling`: ~1 GB of `int[]`,
  ~99% traceable to live Sheet buffers via JFR `OldObjectSample`).
  `AbstractGameComponentEditor` now registers an `EditorListener` that,
  on editor deselect, schedules a 1500 ms grace timer; if the editor
  remains inactive past the grace window the timer calls
  `Sheet.freeCachedResources()` on each sheet and a new
  `SheetViewer.releaseCachedImage()` on each viewer (which nulls the
  viewer's `lastImage` cache that was independently retaining the
  rendered output). On reselect the timer is cancelled and
  `redrawPreview()` re-renders lazily on next paint. The grace timer
  absorbs rapid tab-switching. Verified delta on a 225-component
  project with ~10 editors open: `int[]` dropped from 931 MB to 252 MB
  (-680 MB), heap used 1920 MB → 906 MB (-1 GB), 35,849 → 24,961
  instances. The listener is registered against `AppFrame` rather than
  `this` because per-editor `JComponent.listenerList` does not
  reliably deliver `EditorListener` events under `JInternalFrame`.
- Fixed a macOS bug where Cmd-key menu accelerators (Cmd+S, Cmd+V,
  Cmd+Shift+S, etc.) silently did nothing unless the corresponding menu
  was being actively tracked. With `apple.laf.useScreenMenuBar=true`,
  Cocoa's native key-equivalent dispatch silently drops accelerators for
  non-Aqua menu UIs (FlatLaf), so the JMenuItem's Action never fires
  even though the keystroke does reach Java's
  `KeyboardFocusManager`. The existing keystroke dispatcher in `AppFrame`
  already finds the matching `JMenuItem` (to refresh its enabled state
  before the accelerator is processed), so it now also calls
  `match.doClick(0)` and consumes the event — but only on macOS and only
  when the matched item is enabled. `JMenuBar.isSelected()` cannot gate
  this on Mac because the Apple `ScreenMenuBar` bridge reports the bar
  as permanently selected after the first menu interaction.

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
