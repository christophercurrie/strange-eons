# Changelog

## Unreleased

- Add the `<keybase>-hires-ppi` (or `-hires-dpi`) sheet setting for the resolution of a template's high resolution substitute (default 300).
- Image export print and custom defaults use the component's designed bleed margin and, above 300 ppi, its high resolution template resolution. Saved custom settings still take precedence.
- Add a Miscellaneous Options preference to keep project view state out of `seproject` files. When off, view state moves to `.StrangeEons/seproject-state` the next time a project closes, and that folder can be ignored by source control. On Windows, the folder is also given the hidden attribute.
- Add a root file editor option to choose where Make Bundle touches catalog IDs: in the project folder, or only in the bundled plug-in, which leaves the `eons-plugin` source file unchanged. A plug-in with no ID still has its new ID saved to the project folder.
- Allow `--run` scripts to run without a display, when started with `-Djava.awt.headless=true`. A headless run has no application window or script console, starts libraries and extensions but not other plug-ins, and exits with status 1 if the script fails.
- `--run` scripts no longer bring Strange Eons to the front or take focus on macOS.
- Fix escaped resource files (class maps, tile sets, silhouettes, project task settings and view state): `\=`, `\:`, `\#` and `\!` no longer drop text, `\\` reads as one backslash, and values the app writes read back unchanged.
- `res:` and `project:` URLs resolve through `org.strangeeons.core.resources.ResourceResolver` and work on any class path with `strange-eons-core`, including headless `--run` scripts.
- Add `--project folder`: `project:` URLs resolve against it while no project is open.
- Editor value bindings (`SwingValue`, `SettingBinding`) move to `org.strangeeons.core.editor` and no longer depend on `DIY`.
- The testkit adds `TestEditorPanel`, which builds a component handler's editor without the application so tests can change controls and check settings and changed faces, and `PlainEditorControls`, standard Swing editor controls.
- `ScriptMonkey` no longer has to be first used on the event dispatch thread or after application startup, so scripts can run in unit tests. Script engine settings are reapplied on preference changes by a listener the application registers at startup (`ScriptMonkey.applyEngineSettings`).
- Setting value parsers (yes/no, integers, numbers, cup shapes, text alignment, text styles) move to `org.strangeeons.core.settings.SettingValues`; `Settings` delegates to them. Script values in text styles are evaluated through `Host.evaluateStyleLiteral`.
- The testkit adds `TestSettings`, a standalone `SettingsReadWrite` that parses values as the application does, for testing plug-in code written against the SPI.
- The testkit adds `MarkupImages`, which renders markup to an image with sheet export hints, and `MasterImages`, which checks rendered images against masters (`-Dstrangeeons.updateMasters=true` records them). `ImageComparator` can compare one image with a master file.
- The testkit adds `TestResources`, which resolves `res:` and `project:` URLs against a plug-in's source folders during tests.
- `strange-eons-core` includes the image filters and blend modes (`ca.cgjennings.graphics.filters`, `ca.cgjennings.graphics.composites`), `ca.cgjennings.math`, and the split-join classes of `ca.cgjennings.algo`, under their existing names. `ImageUtilities` delegates its pixel format conversions to `org.strangeeons.core.graphics.ImageFormats`.
- Fix PSD layers that use darken, lighten, hue, saturation, color, luminosity, multiply, screen, overlay, hard light, soft light or difference blending: `BlendMode.derive` threw for every built-in mode.
- Fix `new TintFilter(0, 1, 1).isIdentity()` returning false.
- `strange-eons-core` includes markup layout (`ca.cgjennings.layout`), `resources.StrangeImage`, `VectorImage` and `AbstractVectorImage` under their existing names. Image preparation, SVG loading, stand-in images, font family names and the game locale come from `org.strangeeons.core.Host`, which the application installs at startup; its defaults let markup render without the application.
- Publish `org.strangeeons:strange-eons-core`, application code shared with plug-ins. `EscapedLineReader`, `EscapedLineWriter` and `EscapedTextCodec` move to `org.strangeeons.core.io`, and `LineWrapper` to `org.strangeeons.core.text`; the `ca.cgjennings` names remain, deprecated.
- Publish `org.strangeeons:strange-eons-testkit`, test support for plug-ins. It starts with render comparison against reference masters (`ImageComparator`, `MasterPlan`).

## 3.5.1

- Register `.eon` and `.se*` file associations on the macOS, Windows, and Linux installers, which the jpackage migration had dropped.
- Restore the Script Debugger (`sedebug`) launcher on all platforms and the Windows console launcher (`eons.exe`).
- Note in installer config that the macOS bundle identifier and Windows MSI upgrade GUID changed between install4j and the v3.5.0 jpackage build; upgrading from a pre-3.5 install4j build is a fresh install on both platforms.
- Known limitation: jpackage builds no longer ship the user-editable `.vmoptions` file the install4j builds did. To override JVM options, launch from the command line and prefix each option with `-J` (e.g. `strangeeons -J-Xmx8g`).

## 3.5.0

- Migrate stored YuggothTheme preferences to DagonTheme on upgrade.
- Reset remembered window bounds that no longer fit any attached display.
- Tag the catalog cache by source URL so changing the catalog URL discards the stale cache.

## 3.5.0-beta4

- Combine all configured catalog sources into a single unified view (#3).

## 3.5.0-beta3

- Drop the FlatLight, FlatDark, and NoTheme entries added in beta2.

## 3.5.0-beta2

- Add FlatLaf Light/Dark, IntelliJ, Darcula, and NoTheme themes with per-theme chrome overrides.
- Sign macOS release builds in the publish pipeline (#20, #21).
- Cache plug-in icons during the info probe and null-guard `InstalledPlugin.getIcon()`.
- Resolve update-notification icons through the catalog mapping.
- Scope the release workflow to `v*` tags.

## 3.5.0-beta1

- Plug the post-close editor leak from a `pcl` listener name mismatch (#6).
- Release sheet rasters and viewer caches for inactive editors after a 60s grace (#6).
- Remove wait-cursor flashes around heartbeat-driven renders.
- Plug five remaining `GameComponent` retention paths (#7).
- Recycle per-DIY scripted-plugin engines and strip Rhino-proxy listeners on dispose (#14, #19).
- Upgrade the bundled spelling library to a 1.1 leak-fix build (#19).
- Share the Rhino standard-objects scope across all `SEScriptEngine` instances (#16).
- Add a process-wide compiled-script cache for the JSR-223 engine.
- Memoize JS `{...}` style-literal evaluation.
- Stop dirtying saved components when a plug-in adds a new UI binding.
- Friendlier error when an `.eon` file is corrupt or truncated (#5).
- Fix Cmd-key menu accelerators silently doing nothing under FlatLaf on macOS.
- Sweep reflective JDK-internals shims and drop the `--add-opens=com.apple.laf` carve-out (#1).
- Cut the default catalog URL over from `strangeeons.fizmo.org` to `strangeeons.org`.
- Scope the CI `push` trigger to `main`.
- Add JUnit 5 + Mockito and a regression test pinning the `pcl` property-name match.
