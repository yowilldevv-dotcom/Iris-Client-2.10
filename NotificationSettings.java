package dev.iris.client.notify;

import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ModeSetting;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.setting.SettingGroup;

/** User-facing notification preferences (position, duration, animation, appearance, event filters). */
public final class NotificationSettings extends SettingGroup {
    public final BoolSetting enabled = add(new BoolSetting("Enabled", "Show notifications at all.", true));
    public final ModeSetting position = add(new ModeSetting("Position", "Which screen corner/edge notifications use.",
            "Top Right", "Top Right", "Top Left", "Top Center", "Bottom Right", "Bottom Left", "Bottom Center"));
    public final NumberSetting duration = add(new NumberSetting("Duration", "Seconds a notification stays visible.", 3.0, 1.0, 10.0, 0.5));
    public final ModeSetting animation = add(new ModeSetting("Animation", "How notifications appear and disappear.",
            "Slide", "Slide", "Fade", "Pop", "None"));
    public final NumberSetting maxVisible = add(new NumberSetting("Max visible", "Maximum notifications on screen at once.", 4, 1, 8, 1));
    public final NumberSetting scale = add(new NumberSetting("Scale", "Size of notifications.", 1.0, 0.7, 1.6, 0.05));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", "Background opacity.", 0.92, 0.3, 1.0, 0.02));
    public final BoolSetting compact = add(new BoolSetting("Compact", "Single-line notifications.", false));
    public final BoolSetting progressBar = add(new BoolSetting("Progress bar", "Show a countdown bar.", true));
    public final BoolSetting onModules = add(new BoolSetting("Module toggles", "Notify when modules are enabled/disabled.", true));
    public final BoolSetting onConfigs = add(new BoolSetting("Config events", "Notify when configurations are loaded or saved.", true));
    public final BoolSetting onSettings = add(new BoolSetting("Setting changes", "Notify when a setting is changed by a command.", true));
    public final BoolSetting onCommands = add(new BoolSetting("Command results", "Notify when commands run.", false));
    public final BoolSetting onErrors = add(new BoolSetting("Errors", "Notify when something goes wrong.", true));

    public NotificationSettings() {
        super("Notifications", "Notification position, timing and appearance.");
    }
}
