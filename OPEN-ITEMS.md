# Open items

Deferred follow-ups, ordered roughly by likely impact.

## Memory-leak follow-ups

These were identified while investigating the editor listener leak fixed in
`AbstractGameComponentEditor.dispose()`. They are independent of that fix and
may or may not contribute meaningfully on top of it; worth measuring heap
behavior after the fix lands before prioritizing.

- **JInternalFrame retention.** Maintainer comment at
  `AbstractGameComponentEditor.java:950-952` acknowledges a `JInternalFrame`
  leak that was only partially mitigated by nulling `sheets`, `viewers`, and
  the game component in `dispose()`. Investigate whether the desktop pane,
  the tabbed pane, or Swing internals retain refs to disposed editors.
- **`DefaultPortrait` image retention.** DIY components (e.g. Arkham Horror
  LCG cards) hold many large portrait images. Audit whether portraits are
  cached statically or otherwise outlive the owning editor.
- **DIY editor `ScriptMonkey` bindings.** `DIYEditor` installs the editor and
  game component into script bindings (`ScriptMonkey.VAR_EDITOR`,
  `VAR_COMPONENT`). Confirm these aren't retained by a console registry or
  other long-lived structure across editor lifecycles.
- **`MarkupTargetFactory` static cache.** Single-slot, so it pins at most one
  closed editor at a time — not a multiplying leak, but still a latent issue.
  Clear `cachedComponent`/`cachedTarget` on editor close.

## Test infrastructure

- **Headless mode.** Tests cannot currently run with
  `java.awt.headless=true` because `PlatformSupport.<clinit>` calls
  `Toolkit.getMenuShortcutKeyMask()`, which throws `HeadlessException`.
  Fixing this would let CI run tests on a machine without a display.
- **Byte Buddy / JDK 25.** The Surefire config sets
  `-Dnet.bytebuddy.experimental=true` because Byte Buddy 1.17.x does not yet
  officially recognize JDK 25 bytecode. Drop the flag once Byte Buddy /
  Mockito ship official JDK 25 support.
