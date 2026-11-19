package dev.iris.client.core;

import dev.iris.client.command.CommandManager;
import dev.iris.client.commands.CommandRegistry;
import dev.iris.client.config.ConfigManager;
import dev.iris.client.hud.HudManager;
import dev.iris.client.hud.elements.HudElements;
import dev.iris.client.module.ModuleManager;
import dev.iris.client.modules.ModuleRegistry;
import dev.iris.client.notify.NotificationManager;
import dev.iris.client.notify.NotificationSettings;
import dev.iris.client.setting.SettingGroup;
import dev.iris.client.theme.ThemeManager;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/** Central access point for all Iris subsystems. Created once in {@link dev.iris.client.IrisClient}. */
public final class Iris {
    public static final String MOD_ID = "irisclient";
    public static final String NAME = "Iris 🌌"; // Iris 🌌
    public static final Logger LOGGER = LoggerFactory.getLogger("Iris");

    private static ModuleManager modules;
    private static HudManager hud;
    private static ThemeManager themes;
    private static NotificationManager notifications;
    private static CommandManager commands;
    private static ConfigManager config;
    private static ClientPrefs client;
    private static NotificationSettings notificationSettings;
    private static final Map<String, SettingGroup> GROUPS = new LinkedHashMap<>();
    private static boolean initialized;

    private Iris() {}

    /** Builds every subsystem, registers all content and loads the active configuration. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        client = new ClientPrefs();
        notificationSettings = new NotificationSettings();
        GROUPS.put("client", client);
        GROUPS.put("notifications", notificationSettings);

        themes = new ThemeManager();
        notifications = new NotificationManager(notificationSettings);
        modules = new ModuleManager();
        hud = new HudManager();
        commands = new CommandManager();
        config = new ConfigManager();

        ModuleRegistry.registerAll(modules);
        HudElements.registerAll(hud);
        CommandRegistry.registerAll(commands);

        config.init();
        client.syncAnimations();
        LOGGER.info("{} {} loaded: {} modules, {} HUD elements, {} commands, {} themes",
                NAME, version(), modules.count(), hud.all().size(), commands.count(), themes.all().size());
    }

    public static ModuleManager modules() { return modules; }
    public static HudManager hud() { return hud; }
    public static ThemeManager themes() { return themes; }
    public static NotificationManager notifications() { return notifications; }
    public static CommandManager commands() { return commands; }
    public static ConfigManager config() { return config; }
    public static ClientPrefs client() { return client; }
    public static NotificationSettings notificationSettings() { return notificationSettings; }

    /** Persisted non-module setting groups, keyed by their config section name. */
    public static Map<String, SettingGroup> settingGroups() { return GROUPS; }

    /** Called after a configuration was applied, so derived state can be refreshed. */
    public static void onSettingsReloaded() {
        if (client != null) client.syncAnimations();
    }

    public static String version() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("2.1.0");
    }

    public static String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("26.2");
    }
}
