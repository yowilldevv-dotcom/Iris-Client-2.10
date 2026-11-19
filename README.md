# Iris 🌌 2.1

A modular client-side Fabric mod: ClickGUI, HUD editor, themes, configuration profiles, notifications and commands.
Mod ID: `irisclient` · Minecraft 26.2 · Fabric Loader 0.19.3 · Fabric API 0.152.1+26.2 · Java 25.

## Status: written, NOT yet compiled or run

This source was written without access to Minecraft's libraries, so **it has never been built or launched**.
Treat everything below as "implemented in source, untested". The first `./gradlew build` on a machine with
internet access is the real compile check; expect to fix some Minecraft 26.2 API mismatches (see
[Known risks](#known-risks)). Nothing in this README claims in-game verification.

## Building

```
./gradlew build
```

The JAR is written to `build/libs/irisclient-2.1.0.jar`. Requires JDK 25. Copy it to `.minecraft/mods/` next to
Fabric API. Run a dev client with `./gradlew runClient`.

## What is in the source

| Area | Contents |
|---|---|
| Modules (38) | Movement 10, Render 10, Chat 12, Player 6. All are quality-of-life / information / rendering preferences; there are no combat or competitive-advantage cheats. |
| Settings | Boolean, slider, dropdown, colour (hex + RGB sliders), text, keybind; shared by the GUI, commands and config files. |
| ClickGUI | Draggable category windows (positions saved to `config/irisclient/gui.json`), module search, per-module settings, keybind capture, theme cycling. |
| HUD (16 elements) | Watermark, FPS, CPS, Speed, Health, Memory, Server, Coordinates, Biome, Direction, Clock, Game Time, Ping, Armor Status, Keystrokes, Enabled Modules. |
| HUD editor | Drag with edge/centre snapping, corner-handle and wheel resize, arrow-key nudge, per-element settings, reset. |
| Themes | 12 built-in (Galaxy, Nebula, Midnight, Aurora, Cyberpunk, Ocean, Crimson, Emerald, Classic, Pitch Black, Sunset, Daylight); custom themes can be created, edited, renamed, deleted, exported and imported. |
| Configs | Multiple profiles (modules, settings, keybinds, HUD layout, theme, preferences); create/load/save/rename/duplicate/delete/export/import; atomic writes; corrupt files are quarantined instead of crashing. |
| Notifications | Position, duration, animation, scale, opacity, compact mode, per-event filters. |
| Commands (51) | Prefix `.` in chat or `/iris <command>` with tab completion. `.help` lists them by category. `config`, `theme` and `hud` have subcommands. |
| Screens | Welcome, ClickGUI, HUD editor, Configurations, Themes, Settings, Account. |
| Account screen | Shows only the name and UUID the game already has. It never reads tokens or passwords. |

Default key: **Right Shift** opens the ClickGUI (rebindable in Settings).

## Not done

* The request asked for 100+ modules and 100+ commands. Only 38 modules and 51 commands exist; no filler was added.
* No automated tests, and no in-game testing of any feature.
* No CI workflow (GitHub was removed from scope).
* The 🌌 emoji is not drawn in-game (Minecraft's font cannot render it); the GUI uses a drawn galaxy mark next to the "Iris" wordmark. The emoji is kept in the mod name and README.
* Planned but unwritten: Info, World and Utility module categories, Player/World command groups.

## Known risks

Minecraft 26.2 renamed many rendering and GUI APIs. All version-sensitive calls are concentrated in
`render/Draw`, `gui/IrisScreen`, `util/Input` and `util/Mc`, plus `IrisClient` (Fabric events). If the build fails,
start there. `docs/BUILD_SPEC.md` describes the conventions for adding modules, commands and HUD elements.

## Layout

```
dev.iris.client
  core/      Iris hub, client preferences
  setting/   setting types and groups
  module/    Module base class, manager, categories
  modules/   built-in modules by category
  command/   command framework      commands/  built-in commands
  config/    profiles and safe JSON IO
  theme/     themes and manager
  notify/    notifications
  hud/       HUD framework            hud/elements/  built-in elements
  gui/       screens and widgets
  render/    drawing, colours, animation
  util/      input, formatting, helpers
```

## License

MIT
