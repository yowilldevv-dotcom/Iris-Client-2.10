package dev.iris.client.commands;

import dev.iris.client.command.Command;
import dev.iris.client.command.CommandCategory;
import dev.iris.client.command.CommandContext;
import dev.iris.client.command.CommandException;
import dev.iris.client.command.CommandManager;
import dev.iris.client.core.Iris;
import dev.iris.client.module.Category;
import dev.iris.client.module.Module;
import dev.iris.client.setting.KeyNames;
import dev.iris.client.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/** Commands that manage modules: toggling, settings, keybinds, search. Also registers help. */
public final class ModuleCommands {
    private ModuleCommands() {}

    public static void register(CommandManager cm) {
        cm.register(new Help(), new Toggle(), new Enable(), new Disable(), new Modules(), new Info(),
                new Set(), new Get(), new Reset(), new Bind(), new Unbind(), new Binds(), new Search(),
                new Enabled(), new DisableAll());
    }

    private static List<String> moduleNames() {
        List<String> out = new ArrayList<>();
        for (Module m : Iris.modules().all()) out.add(m.id());
        return out;
    }

    private static List<String> settingKeys(CommandContext ctx) {
        Module m = Iris.modules().get(ctx.args().length > 0 ? ctx.args()[0] : "");
        List<String> out = new ArrayList<>();
        if (m != null) for (Setting<?> s : m.settings()) out.add(s.key());
        return out;
    }

    static final class Help extends Command {
        Help() { super("help", CommandCategory.CLIENT, "Lists commands or explains one.", "[command|category]", "?", "commands"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            String p = CommandContext.prefix();
            if (ctx.has(0)) {
                Command c = Iris.commands().find(ctx.arg(0));
                if (c != null) {
                    ctx.reply("§b" + p + c.name() + " §f" + c.usage());
                    ctx.line("  §7" + c.description());
                    if (c.aliases().length > 0) ctx.line("  §7Aliases: " + String.join(", ", c.aliases()));
                    return;
                }
                CommandCategory cat = CommandCategory.parse(ctx.arg(0));
                if (cat == null) throw new CommandException("No command or category '" + ctx.arg(0) + "'.");
                ctx.reply("§b" + cat.displayName() + " commands:");
                for (Command x : Iris.commands().byCategory(cat)) ctx.line("  §f" + p + x.name() + " §7- " + x.description());
                return;
            }
            ctx.reply("§bIris commands §7(" + Iris.commands().count() + "). Use §f" + p + "help <category|command>§7.");
            for (CommandCategory cat : CommandCategory.values()) {
                List<Command> list = Iris.commands().byCategory(cat);
                if (list.isEmpty()) continue;
                StringBuilder sb = new StringBuilder("  §b" + cat.displayName() + "§7: §f");
                for (int i = 0; i < list.size(); i++) sb.append(i > 0 ? ", " : "").append(list.get(i).name());
                ctx.line(sb.toString());
            }
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String partial) {
            List<String> out = new ArrayList<>();
            for (Command c : Iris.commands().all()) out.add(c.name());
            for (CommandCategory c : CommandCategory.values()) out.add(c.displayName().toLowerCase());
            return i == 0 ? out : List.of();
        }
    }

    static final class Toggle extends Command {
        Toggle() { super("toggle", CommandCategory.MODULES, "Toggles a module.", "<module>", "t"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            Module m = ctx.module(0);
            m.toggle();
            ctx.ok(m.name() + " is now " + (m.isEnabled() ? "enabled" : "disabled") + ".");
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? moduleNames() : List.of(); }
    }

    static final class Enable extends Command {
        Enable() { super("enable", CommandCategory.MODULES, "Enables a module.", "<module>", "on"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            Module m = ctx.module(0);
            m.setEnabled(true);
            if (m.isEnabled()) ctx.ok(m.name() + " enabled."); else throw new CommandException(m.name() + " could not be enabled.");
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? moduleNames() : List.of(); }
    }

    static final class Disable extends Command {
        Disable() { super("disable", CommandCategory.MODULES, "Disables a module.", "<module>", "off"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            Module m = ctx.module(0);
            m.setEnabled(false);
            ctx.ok(m.name() + " disabled.");
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? moduleNames() : List.of(); }
    }

    static final class Modules extends Command {
        Modules() { super("modules", CommandCategory.MODULES, "Lists modules, optionally of one category.", "[category]", "mods"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            if (ctx.has(0)) {
                Category cat = Category.parse(ctx.arg(0));
                if (cat == null) throw new CommandException("Unknown category '" + ctx.arg(0) + "'.");
                list(ctx, cat);
                return;
            }
            for (Category c : Category.values()) list(ctx, c);
        }

        private void list(CommandContext ctx, Category cat) {
            List<Module> mods = Iris.modules().byCategory(cat);
            if (mods.isEmpty()) return;
            StringBuilder sb = new StringBuilder("§b" + cat.displayName() + " §7(" + mods.size() + ")§f: ");
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                sb.append(i > 0 ? "§7, " : "").append(m.isEnabled() ? "§a" : "§f").append(m.name());
            }
            ctx.line(sb.toString());
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) {
            List<String> out = new ArrayList<>();
            if (i == 0) for (Category c : Category.values()) out.add(c.displayName().toLowerCase());
            return out;
        }
    }

    static final class Info extends Command {
        Info() { super("info", CommandCategory.MODULES, "Shows a module's description, key and settings.", "<module>", "module"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            Module m = ctx.module(0);
            ctx.reply("§b" + m.name() + " §7[" + m.category().displayName() + "] " + (m.isEnabled() ? "§aenabled" : "§cdisabled"));
            ctx.line("  §7" + m.description());
            ctx.line("  §7Key: §f" + KeyNames.name(m.key()));
            for (Setting<?> s : m.settings()) ctx.line("  §f" + s.key() + " §7= §f" + s.display() + " §8(" + s.description() + ")");
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? moduleNames() : List.of(); }
    }

    static final class Set extends Command {
        Set() { super("set", CommandCategory.MODULES, "Changes a module setting.", "<module> <setting> <value>"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(3);
            Module m = ctx.module(0);
            Setting<?> s = m.find(ctx.arg(1)).orElseThrow(() -> new CommandException("No setting '" + ctx.args()[1] + "' on " + m.name() + "."));
            if (!s.parse(ctx.joinFrom(2))) {
                throw new CommandException("'" + ctx.joinFrom(2) + "' is not valid for " + s.key() + (s.suggestions().isEmpty() ? "." : " (try " + String.join(", ", s.suggestions()) + ")."));
            }
            ctx.ok(m.name() + " " + s.key() + " = " + s.display());
            Iris.notifications().settingChanged(m.name(), s.name(), s.display());
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) {
            if (i == 0) return moduleNames();
            if (i == 1) return settingKeys(ctx);
            if (i == 2) {
                Module m = Iris.modules().get(ctx.args()[0]);
                if (m != null) return m.find(ctx.args()[1]).map(Setting::suggestions).orElse(List.of());
            }
            return List.of();
        }
    }

    static final class Get extends Command {
        Get() { super("get", CommandCategory.MODULES, "Shows one module setting.", "<module> <setting>"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(2);
            Module m = ctx.module(0);
            Setting<?> s = m.find(ctx.arg(1)).orElseThrow(() -> new CommandException("No setting '" + ctx.args()[1] + "' on " + m.name() + "."));
            ctx.reply(m.name() + " " + s.key() + " = §f" + s.display() + " §7(default " + String.valueOf(s.defaultValue()) + ")");
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) {
            return i == 0 ? moduleNames() : i == 1 ? settingKeys(ctx) : List.of();
        }
    }

    static final class Reset extends Command {
        Reset() { super("reset", CommandCategory.MODULES, "Resets a module's settings to defaults.", "<module>"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            Module m = ctx.module(0);
            m.resetSettings();
            ctx.ok("Reset settings of " + m.name() + ".");
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? moduleNames() : List.of(); }
    }

    static final class Bind extends Command {
        Bind() { super("bind", CommandCategory.MODULES, "Binds a key to toggle a module.", "<module> <key>"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(2);
            Module m = ctx.module(0);
            int code = KeyNames.parse(ctx.arg(1));
            if (code == Integer.MIN_VALUE) throw new CommandException("Unknown key '" + ctx.arg(1) + "'.");
            m.setKey(code);
            ctx.ok(m.name() + " bound to " + KeyNames.name(code) + ".");
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) {
            return i == 0 ? moduleNames() : i == 1 ? KeyNames.all() : List.of();
        }
    }

    static final class Unbind extends Command {
        Unbind() { super("unbind", CommandCategory.MODULES, "Removes a module's keybind.", "<module>"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            Module m = ctx.module(0);
            m.setKey(KeyNames.NONE);
            ctx.ok(m.name() + " unbound.");
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? moduleNames() : List.of(); }
    }

    static final class Binds extends Command {
        Binds() { super("binds", CommandCategory.MODULES, "Lists all module keybinds.", "", "keybinds"); }

        @Override public void execute(CommandContext ctx) {
            int n = 0;
            for (Module m : Iris.modules().all()) {
                if (m.key() < 0) continue;
                ctx.line("  §f" + m.name() + " §7- §b" + KeyNames.name(m.key()));
                n++;
            }
            ctx.reply(n == 0 ? "No module has a keybind." : n + " keybind(s).");
        }
    }

    static final class Search extends Command {
        Search() { super("search", CommandCategory.MODULES, "Searches modules by name or description.", "<text>", "find"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            List<Module> hits = Iris.modules().search(ctx.joinFrom(0));
            if (hits.isEmpty()) throw new CommandException("No module matches '" + ctx.joinFrom(0) + "'.");
            for (Module m : hits) ctx.line("  §f" + m.name() + " §7- " + m.description());
        }
    }

    static final class Enabled extends Command {
        Enabled() { super("enabled", CommandCategory.MODULES, "Lists enabled modules.", "", "active"); }

        @Override public void execute(CommandContext ctx) {
            Module[] on = Iris.modules().enabled();
            if (on.length == 0) { ctx.reply("No modules are enabled."); return; }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < on.length; i++) sb.append(i > 0 ? "§7, §f" : "§f").append(on[i].name());
            ctx.reply(on.length + " enabled: " + sb);
        }
    }

    static final class DisableAll extends Command {
        DisableAll() { super("disableall", CommandCategory.MODULES, "Turns every module off.", "", "panic"); }

        @Override public void execute(CommandContext ctx) {
            ctx.ok("Disabled " + Iris.modules().disableAll() + " module(s).");
        }
    }
}
