package dev.iris.client.commands;

import dev.iris.client.command.Command;
import dev.iris.client.command.CommandCategory;
import dev.iris.client.command.CommandContext;
import dev.iris.client.command.CommandException;
import dev.iris.client.command.CommandManager;
import dev.iris.client.core.Iris;
import dev.iris.client.gui.Screens;
import dev.iris.client.setting.ColorSetting;
import dev.iris.client.theme.Theme;
import dev.iris.client.util.Mc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Commands for selecting and editing GUI themes. */
public final class ThemeCommands {
    private ThemeCommands() {}

    public static void register(CommandManager cm) {
        cm.register(new ThemeCmd());
    }

    static final class ThemeCmd extends Command {
        ThemeCmd() {
            super("theme", CommandCategory.THEME, "Selects, creates and edits GUI themes.",
                    "<list|set|current|create|rename|delete|export|import|edit|tokens|gui> [args]", "themes");
        }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            String sub = ctx.arg(0).toLowerCase();
            try {
                switch (sub) {
                    case "list" -> {
                        StringBuilder sb = new StringBuilder();
                        for (Theme t : Iris.themes().all()) {
                            sb.append(sb.length() > 0 ? "§7, " : "").append(t == Iris.themes().current() ? "§a" : "§f").append(t.name())
                                    .append(t.isBuiltin() ? "" : "§7*");
                        }
                        ctx.reply("Themes: " + sb + " §7(* = custom)");
                    }
                    case "current" -> ctx.reply("Current theme: §f" + Iris.themes().current().name());
                    case "set" -> {
                        ctx.require(2);
                        Theme t = ctx.theme(1);
                        Iris.themes().setCurrent(t.name());
                        ctx.ok("Theme set to " + t.name() + ".");
                    }
                    case "create" -> {
                        ctx.require(2);
                        Theme base = Iris.themes().current();
                        Theme t = Iris.themes().create(ctx.joinFrom(1), base);
                        Iris.themes().setCurrent(t.name());
                        ctx.ok("Created '" + t.name() + "' from " + base.name() + " and selected it.");
                    }
                    case "rename" -> {
                        ctx.require(3);
                        Iris.themes().rename(ctx.arg(1), ctx.joinFrom(2));
                        ctx.ok("Renamed to '" + ctx.joinFrom(2) + "'.");
                    }
                    case "delete" -> {
                        ctx.require(2);
                        Iris.themes().delete(ctx.joinFrom(1));
                        ctx.ok("Deleted theme.");
                    }
                    case "export" -> {
                        ctx.require(2);
                        Path dest = ConfigCommands.exportPath(ctx.has(2) ? ctx.arg(2) : ctx.arg(1) + ".theme", ".json");
                        Iris.themes().exportTo(ctx.arg(1), dest);
                        ctx.ok("Exported to " + dest);
                    }
                    case "import" -> {
                        ctx.require(2);
                        Theme t = Iris.themes().importFrom(ConfigCommands.exportPath(ctx.arg(1), ".json"));
                        ctx.ok("Imported theme '" + t.name() + "'.");
                    }
                    case "edit" -> {
                        ctx.require(3);
                        Theme t = Iris.themes().current();
                        if (t.isBuiltin()) throw new CommandException("Built-in themes cannot be edited. Use theme create <name> first.");
                        String token = ctx.arg(1);
                        if (!Theme.TOKENS.contains(token)) throw new CommandException("Unknown token. Try theme tokens.");
                        Integer c = ColorSetting.parseColor(ctx.arg(2));
                        if (c == null) throw new CommandException("'" + ctx.arg(2) + "' is not a color (#RRGGBB or #AARRGGBB).");
                        t.set(token, c);
                        Iris.themes().save(t);
                        ctx.ok(token + " = " + ColorSetting.hex(c));
                    }
                    case "tokens" -> ctx.reply("Tokens: " + String.join(", ", Theme.TOKENS));
                    case "gui" -> Mc.openScreen(Screens.themes());
                    default -> throw new CommandException("Unknown subcommand '" + sub + "'.", true);
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                throw new CommandException(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) {
            if (i == 0) return List.of("list", "set", "current", "create", "rename", "delete", "export", "import", "edit", "tokens", "gui");
            String sub = ctx.args()[0].toLowerCase();
            if (i == 1 && List.of("set", "rename", "delete", "export").contains(sub)) return Iris.themes().names();
            if (i == 1 && sub.equals("edit")) return Theme.TOKENS;
            return List.of();
        }
    }
}
