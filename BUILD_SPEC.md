# Iris 🌌 2.1 — build spec for contributors

Project root: `/home/claude/iris-client` · Package root: `dev.iris.client` · Java 25 · Minecraft 26.2 (unobfuscated,
**Mojang names**, e.g. `Identifier` not `ResourceLocation`) · Fabric Loader 0.19.3 · Fabric API 0.152.1+26.2 · no mixins.

## Read first (do not modify these; report needed changes instead)
`core/Iris.java`, `core/ClientPrefs.java`, `setting/*`, `module/Module.java`, `module/ModuleManager.java`,
`module/Category.java`, `hud/HudElement.java`, `hud/TextHudElement.java`, `hud/HudManager.java`,
`command/*`, `config/ConfigManager.java`, `theme/*`, `notify/*`, `render/Draw.java`, `render/Colors.java`,
`render/Anim.java`, `util/*`, `gui/IrisScreen.java`, `gui/Ui.java`, `gui/TextField.java`.

## Hard rules
1. **Nothing can be compiled in this environment.** Write code you have mentally compiled. Prefer boring, certain APIs.
   Keep Minecraft API usage small. All drawing goes through `Draw`; all screen input through `IrisScreen`; window/key
   polling through `Input`; world/chat helpers through `Mc`. If you add a *new* version-sensitive Minecraft call outside
   those adapters, keep it tiny, and list it in your report under `VERSION-SENSITIVE CALLS` (file + call).
2. **Real features only.** No placeholders, no message-only stubs, no duplicate-by-function entries, no filler to hit a
   count. If you cannot implement something with APIs you are confident about, skip it and say so.
3. **No server-side cheating features.** Do not write fly, speed, kill aura, reach, x-ray, no-fall, scaffold, aimbot,
   auto-clicker, velocity/knockback changes, ESP-through-walls, or anything giving a competitive advantage on servers.
   Scope is quality-of-life, information, rendering preferences, chat tools, utilities.
4. **Modules**: extend `Module`, declare settings with `add(...)` in the constructor, undo everything in `onDisable()`
   (restore game options, reset key states), never start threads, avoid per-tick allocations in hot paths, never iterate
   all entities more than once per tick (and cap work). Use `info()` to show the current mode/value in the list.
   Unique names (case-insensitive). Descriptions are one clear sentence.
5. **Commands**: extend `Command`; register in the file's `register(CommandManager)`. Validate input with
   `CommandContext` helpers; throw `CommandException` for user errors; set `requirement()` where a world/singleplayer/
   operator is needed; implement `suggest(...)` for every argument that has a finite or discoverable set of values.
   Output with `ctx.reply/ok/warn/error/line`. Unique names *and aliases* across the whole client.
6. Comments: a short class Javadoc and comments only where logic is non-obvious. Consistent naming. Handle exceptions.
7. Only create/edit the files you own. Do not touch `Iris.java`, registries or build files.
8. Settings changes made by commands should call `Iris.notifications().settingChanged(owner, setting, value)`.

## Known-good Minecraft API (Mojang names; use these with confidence)
`Minecraft.getInstance()`, `mc.player` (LocalPlayer), `mc.level` (ClientLevel), `mc.options` (Options; key mappings
`keyUp keyDown keyLeft keyRight keyJump keyShift keySprint keyAttack keyUse`, each `.isDown()` / `.setDown(boolean)`),
`mc.font`, `mc.gui`, `mc.screen`, `mc.getWindow()`, `player.getX()/getY()/getZ()`, `blockPosition()`, `getYRot()`,
`getXRot()`, `getDeltaMovement()`, `getHealth()`, `getMaxHealth()`, `getFoodData().getFoodLevel()`,
`getItemBySlot(EquipmentSlot.HEAD/CHEST/LEGS/FEET/MAINHAND)`, `getMainHandItem()`, `ItemStack` (`isEmpty()`,
`getCount()`, `getMaxDamage()`, `getDamageValue()`, `isDamageableItem()`, `getHoverName().getString()`),
`level.getGameTime()`, `level.getDayTime()`, `level.getBiome(pos)` → `Holder` with `getRegisteredName()` (string like
`minecraft:plains`; use `Format.prettyId`), `net.minecraft.network.chat.Component.literal(...)`.
Screens: methods are `extractRenderState(GuiGraphicsExtractor, int, int, float)`; open with `Mc.openScreen(screen)`.
Input events are `MouseButtonEvent` / `KeyEvent` / `CharacterEvent` (already handled inside `IrisScreen`).

## Shared contracts between workers
* `gui/Screens.java` (static factory, written by GUI worker 1): `welcome()`, `clickGui()`, `hudEditor()`, `configs()`,
  `themes()`, `account()`, `settings()` each returning `net.minecraft.client.gui.screens.Screen`.
  `settings()` = client + notification preferences. Commands open screens with `Mc.openScreen(Screens.xxx())`.
* `gui/SettingsPanel.java` (GUI worker 1): renders and edits every setting of one `SettingGroup`.
  ```java
  public SettingsPanel(SettingGroup group)
  public float layout(float x, float y, float width)        // positions controls, returns total height
  public void draw(Draw d, float mouseX, float mouseY)
  public boolean mouseDown(double x, double y, int button)  // true if consumed
  public void mouseUp(double x, double y, int button)
  public void mouseDrag(double x, double y, int button)
  public boolean keyDown(int key, int scancode, int modifiers) // keybind capture / text editing; true if consumed
  public void charTyped(int codePoint)
  public boolean capturingInput()                            // text field focused or waiting for a key
  ```
  Supports Bool (toggle), Number (slider; drag), Mode (dropdown), Color (hex field + RGB sliders + swatch), Text
  (text field), Keybind (click then press a key; Escape = cancel; Backspace/Delete = unbind). Honors `Setting.isVisible()`.
* `hud/elements/HudElements.java` (HUD worker): `public static void registerAll(HudManager hud)`.
* `modules/<Category>Modules.java`: `public static void register(ModuleManager mm)`, categories: Movement, Player, Render,
  World, Chat, Info, Utility, Client. `commands/<Area>Commands.java`: `public static void register(CommandManager cm)`;
  areas: Module, Config, Theme, Hud, Client, Player, World, Info, Utility.

## Final report format (keep it short)
1. Files written. 2. Names of every module/command/element/screen. 3. `VERSION-SENSITIVE CALLS`. 4. Anything skipped.
