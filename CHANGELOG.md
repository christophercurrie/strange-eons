# Changelog

## 3.5.0-beta1

- Plugged the long-standing post-close memory leak: `AbstractGameComponentEditor` now removes its `pcl` listener under the property name it actually registered with, so closed editors no longer stay pinned in `AppFrame` (issue #6 root cause).
- Released sheet rasters and viewer image caches for inactive editors after a 60 s grace window, dropping working-set heap by ~1 GB on large projects (issue #6).
- Removed wait-cursor flashes around heartbeat-driven and synchronous renders so normal editing no longer flickers a spinner.
- Plugged five remaining `GameComponent` retention paths (weak-ref `lastTarget`, removed `MarkupTargetFactory` cache, eager `ContextBar` clear, dropped `Settings.tlStyleEvaluator` ThreadLocal, `PortraitPanel.removeNotify`) so closed editors stop anchoring Rhino reflection graphs (issue #7).
- Added on-demand recycling of per-DIY scripted-plugin engines plus stripping of Rhino-proxy listeners on editor dispose, releasing 10+ engines per session that previously stayed pinned (issues #14, #19).
- Bundled spelling library upgraded to a 1.1 leak-fix build that lets `JSpellingTextField`/`Area`/`ComboBox` deregister their shared-instance listeners on detach (issue #19).
- Shared the Rhino standard-objects scope across all `SEScriptEngine` instances, cutting `NativeJavaMethod`/`MemberBox`/slot-map counts ~85% on plugin-heavy boots (issue #16).
- Added a process-wide compiled-script cache for the JSR-223 engine: bootstrap-library re-eval drops 5–25× and component-open construction time drops ~5×, with 97% hit rate on real plug-in mixes.
- Memoized JS `{...}` style-literal evaluation so a typical AHLCG deck open creates 92 `ScriptMonkey` instances instead of 5,422.
- Stopped the editor from immediately marking saved components dirty when a plug-in adds a new UI binding to an existing component type.
- Friendlier failure message when an `.eon` file is corrupt or truncated, instead of a raw stack trace (issue #5).
- Fixed a macOS bug where Cmd-key menu accelerators (Cmd+S, Cmd+V, etc.) silently did nothing under FlatLaf with the screen menu bar enabled.
- Swept three reflective JDK-internals shims (`AWTUtilities`, `OSXAdapter`, Aqua menu UI copy) and dropped the matching `--add-opens=com.apple.laf` carve-out from all run/jpackage profiles (issue #1).
- Cut over the default update-catalog URL from `strangeeons.fizmo.org` to `strangeeons.org`, now driven by the new `strange-eons-registry` repo with a 301 redirect for legacy installs.
- Scoped the CI workflow's `push` trigger to `main` so feature-branch pushes no longer duplicate the PR run.
- Added JUnit 5 + Mockito as test dependencies and a regression test pinning the `pcl` property-name match.
