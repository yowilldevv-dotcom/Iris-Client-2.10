package dev.iris.client.module;

import dev.iris.client.core.Iris;
import dev.iris.client.notify.NotificationManager;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.KeyNames;
import dev.iris.client.setting.SettingGroup;
import net.minecraft.client.Minecraft;

/**
 * Base class for every Iris module. Subclasses declare settings with {@link #add} in their constructor and
 * override the lifecycle hooks they need. The {@link ModuleManager} only calls hooks on enabled modules and
 * isolates exceptions, so a faulty module can never crash the game loop.
 *
 * <p>Contract: whatever {@link #onEnable()} changes (options, fields, key states) must be undone in
 * {@link #onDisable()}. The manager also calls {@code onDisable()} on shutdown for every enabled module.
 */
public abstract class Module extends SettingGroup {
    protected final Minecraft mc = Minecraft.getInstance();

    private final Category category;
    private boolean enabled;
    private int key = KeyNames.NONE;
    private boolean shownInList = true;
    boolean keyWasDown;
    int consecutiveErrors;

    protected Module(String name, String description, Category category) {
        super(name, description);
        this.category = category;
    }

    public Category category() { return category; }
    public boolean isEnabled() { return enabled; }
    public int key() { return key; }
    public boolean isShownInList() { return shownInList; }

    public void setKey(int key) {
        this.key = key < 0 ? KeyNames.NONE : key;
        dev.iris.client.config.ConfigManager.markDirty();
    }

    public void setShownInList(boolean shown) {
        this.shownInList = shown;
        dev.iris.client.config.ConfigManager.markDirty();
    }

    /** Text appended after the name in the enabled-modules list, e.g. a mode name. Empty for none. */
    public String info() { return ""; }

    /** When true the module only ticks while a world is loaded. */
    protected boolean requiresWorld() { return true; }

    public final boolean toggle() {
        setEnabled(!enabled);
        return enabled;
    }

    public final void setEnabled(boolean value) { setEnabled(value, true); }

    /**
     * Enables or disables the module.
     *
     * @param notify whether to show a notification (false while loading configurations)
     */
    public final void setEnabled(boolean value, boolean notify) {
        if (value == enabled) return;
        if (value) {
            try {
                enabled = true;
                consecutiveErrors = 0;
                Iris.modules().stateChanged(this);
                onEnable();
            } catch (Throwable t) {
                Iris.LOGGER.error("Module {} failed to enable", name(), t);
                enabled = false;
                Iris.modules().stateChanged(this);
                safeDisableHook();
                Iris.notifications().error("Module error", name() + " could not be enabled: " + t.getClass().getSimpleName());
                return;
            }
        } else {
            enabled = false;
            Iris.modules().stateChanged(this);
            safeDisableHook();
        }
        dev.iris.client.config.ConfigManager.markDirty();
        if (notify) {
            Iris.notifications().moduleToggled(name(), enabled);
        }
    }

    /** Runs {@link #onDisable()} without letting exceptions escape. */
    final void safeDisableHook() {
        try {
            onDisable();
        } catch (Throwable t) {
            Iris.LOGGER.error("Module {} failed during cleanup", name(), t);
        }
    }

    boolean ticksInCurrentState() {
        return !requiresWorld() || (mc.player != null && mc.level != null);
    }

    // ---- lifecycle hooks (override as needed) ----

    protected void onEnable() {}

    protected void onDisable() {}

    /** Called once per client tick while enabled (and in a world, unless {@link #requiresWorld()} is false). */
    public void onTick() {}

    /** Called every rendered frame while enabled, after the world, for 2D overlays. */
    public void onRender(Draw draw, float partialTick) {}

    /** Called when the player leaves a world or server; reset any per-world state here. */
    public void onWorldLeave() {}

    /** Incoming game/chat message hook. Return false to hide the message. {@code plain} is the unformatted text. */
    public boolean allowIncomingChat(String plain) { return true; }

    /** Outgoing chat hook. Return the (possibly modified) message, or null to cancel sending it. */
    public String onOutgoingChat(String message) { return message; }

    protected NotificationManager notifications() { return Iris.notifications(); }
}
