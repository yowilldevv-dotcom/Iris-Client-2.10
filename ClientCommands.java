package dev.iris.client.commands;

import dev.iris.client.command.Command;
import dev.iris.client.command.CommandCategory;
import dev.iris.client.command.CommandContext;
import dev.iris.client.command.CommandException;
import dev.iris.client.command.CommandManager;
import dev.iris.client.core.Iris;
import dev.iris.client.gui.Screens;
import dev.iris.client.notify.NotificationManager;
import dev.iris.client.setting.Setting;
import dev.iris.client.setting.SettingGroup;
import dev.iris.client.util.Mc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Commands for Iris itself: screens, preferences, notifications, reload. */
public final class ClientCommands {
    private ClientCommands() {}

    public static void register(CommandManager cm) {
        cm.register(new Gui(), new SettingsCmd(), new Account(), new Welcome(), new Version(), new Reload(),
                new ClientSet(), new NotifySet(), new NotifyTest(), new Prefix());
    }

    /** Shared implementation for "set a value on a SettingGroup" commands. */
    private static void setOn(CommandContext ctx, SettingGroup group) throws CommandException {
        ctx.require(2);
        Setting<?> s = group.find(ctx.arg(0)).orElseThrow(() -> new CommandException("No setting '" + ctx.args()[0] + "'."));
        if (!s.parse(ctx.joinFrom(1))) throw new CommandException("'" + ctx.joinFrom(1) + "' is not valid for " + s.key() + ".");
        ctx.ok(s.key() + " = " + s.display());
        Iris.notifications().settingChanged(group.name(), s.name(), s.display());
    }

    private static List<String> keys(SettingGroup g, int i, CommandContext ctx) {
        List<String> out = new ArrayList<>();
        if (i == 0) for (Setting<?> s : g.settings()) out.add(s.key());
        else if (i == 1 && ctx.has(0)) g.find(ctx.args()[0]).ifPresent(s -> out.addAll(s.suggestions()));
        return out;
    }

    static final class Gui extends Command {
        Gui() { super("gui", CommandCategory.CLIENT, "Opens the ClickGUI.", "", "clickgui"); }
        @Override public void execute(CommandContext ctx) { Mc.openScreen(Screens.clickGui()); }
    }

    static final class SettingsCmd extends Command {
        SettingsCmd() { super("settings", CommandCategory.CLIENT, "Opens the Iris settings screen.", "", "prefs"); }
        @Override public void execute(CommandContext ctx) { Mc.openScreen(Screens.settings()); }
    }

    static final class Account extends Command {
        Account() { super("account", CommandCategory.CLIENT, "Opens the account and profile screen.", "", "profile-screen"); }
        @Override public void execute(CommandContext ctx) { Mc.openScreen(Screens.account()); }
    }

    static final class Welcome extends Command {
        Welcome() { super("welcome", CommandCategory.CLIENT, "Opens the welcome screen.", ""); }
        @Override public void execute(CommandContext ctx) { Mc.openScreen(Screens.welcome()); }
    }

    static final class Version extends Command {
        Version() { super("version", CommandCategory.CLIENT, "Shows client and game versions.", "", "ver"); }

        @Override public void execute(CommandContext ctx) {
            ctx.reply(Iris.NAME + " §f" + Iris.version() + " §7on Minecraft §f" + Iris.minecraftVersion() + " §7(" + Iris.modules().count()
                    + " modules, " + Iris.hud().all().size() + " HUD elements, " + Iris.commands().count() + " commands)");
        }
    }

    static final class Reload extends Command {
        Reload() { super("reload", CommandCategory.CLIENT, "Reloads the active configuration from disk.", ""); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            try {
                String active = Iris.config().activeName();
                if (!Iris.config().exists(active)) throw new CommandException("Active config file is missing.");
                // load() saves the active config first; saving then reloading would discard disk edits, so skip the save.
                Iris.config().applyDefaults();
                Iris.config().load(active);
                ctx.ok("Reloaded '" + active + "'.");
            } catch (IOException | IllegalArgumentException e) {
                throw new CommandException(e.getMessage());
            }
        }
    }

    static final class ClientSet extends Command {
        ClientSet() { super("clientset", CommandCategory.CLIENT, "Changes a client preference (autosave, animations, ...).", "<setting> <value>", "pref"); }
        @Override public void execute(CommandContext ctx) throws CommandException { setOn(ctx, Iris.client()); }
        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return keys(Iris.client(), i, ctx); }
    }

    static final class NotifySet extends Command {
        NotifySet() { super("notifyset", CommandCategory.CLIENT, "Changes a notification setting (position, duration, ...).", "<setting> <value>", "notify"); }
        @Override public void execute(CommandContext ctx) throws CommandException { setOn(ctx, Iris.notificationSettings()); }
        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return keys(Iris.notificationSettings(), i, ctx); }
    }

    static final class NotifyTest extends Command {
        NotifyTest() { super("notifytest", CommandCategory.CLIENT, "Shows a test notification.", "[info|success|warning|error]"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            String kind = ctx.argOr(0, "info").toLowerCase();
            NotificationManager n = Iris.notifications();
            switch (kind) {
                case "info" -> n.info("Iris", "This is a test notification.");
                case "success" -> n.success("Iris", "Everything works.");
                case "warning" -> n.warn("Iris", "This is a warning.");
                case "error" -> n.post(NotificationManager.Type.ERROR, "Iris", "This is an error test.");
                default -> throw new CommandException("Unknown type '" + kind + "'.", true);
            }
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? List.of("info", "success", "warning", "error") : List.of(); }
    }

    static final class Prefix extends Command {
        Prefix() { super("prefix", CommandCategory.CLIENT, "Shows or changes the chat command prefix.", "[new prefix]"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            if (!ctx.has(0)) { ctx.reply("Prefix is '" + CommandContext.prefix() + "'."); return; }
            String p = ctx.arg(0);
            if (p.startsWith("/") || p.isBlank()) throw new CommandException("Prefix cannot be empty or start with '/'.");
            Iris.client().prefix.set(p);
            ctx.ok("Prefix is now '" + Iris.client().prefix.get() + "'.");
        }
    }
}
