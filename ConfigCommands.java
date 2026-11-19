package dev.iris.client.commands;

import dev.iris.client.command.Command;
import dev.iris.client.command.CommandCategory;
import dev.iris.client.command.CommandContext;
import dev.iris.client.command.CommandException;
import dev.iris.client.command.CommandManager;
import dev.iris.client.core.Iris;
import dev.iris.client.gui.Screens;
import dev.iris.client.util.Mc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Commands for the multi-profile configuration manager. */
public final class ConfigCommands {
    private ConfigCommands() {}

    public static void register(CommandManager cm) {
        cm.register(new Config());
    }

    /** Resolves a user path: relative paths live under config/irisclient/exports and may not escape it. */
    static Path exportPath(String text, String suffix) throws CommandException {
        Path base = Iris.config().root().resolve("exports").toAbsolutePath().normalize();
        Path p = Path.of(text);
        if (p.isAbsolute()) return p.normalize();
        Path resolved = base.resolve(p.toString().endsWith(".json") ? p.toString() : p + suffix).normalize();
        if (!resolved.startsWith(base)) throw new CommandException("Relative paths must stay inside " + base + ".");
        return resolved;
    }

    static final class Config extends Command {
        Config() {
            super("config", CommandCategory.CONFIG, "Manages configuration profiles.",
                    "<list|current|save|load|create|rename|duplicate|delete|export|import|reset|gui> [args]", "cfg", "profile");
        }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            String sub = ctx.arg(0).toLowerCase();
            try {
                switch (sub) {
                    case "list" -> {
                        List<String> all = Iris.config().list();
                        ctx.reply("Configs (" + all.size() + "): " + String.join(", ", all.stream()
                                .map(n -> n.equals(Iris.config().activeName()) ? "§a" + n + "§f" : n).toList()));
                    }
                    case "current" -> ctx.reply("Active config: §f" + Iris.config().activeName());
                    case "save" -> {
                        Iris.config().saveActive();
                        ctx.ok("Saved '" + Iris.config().activeName() + "'.");
                        Iris.notifications().configEvent("Config saved", Iris.config().activeName());
                    }
                    case "load" -> {
                        ctx.require(2);
                        List<String> warnings = Iris.config().load(ctx.joinFrom(1));
                        ctx.ok("Loaded '" + Iris.config().activeName() + "'.");
                        if (!warnings.isEmpty()) ctx.warn(warnings.size() + " entries were invalid and skipped (see log).");
                        warnings.forEach(w -> Iris.LOGGER.warn("Config warning: {}", w));
                    }
                    case "create" -> {
                        ctx.require(2);
                        Iris.config().create(ctx.joinFrom(1));
                        ctx.ok("Created '" + ctx.joinFrom(1) + "' with default settings. Load it with config load.");
                    }
                    case "rename" -> {
                        ctx.require(3);
                        Iris.config().rename(ctx.arg(1), ctx.joinFrom(2));
                        ctx.ok("Renamed to '" + ctx.joinFrom(2) + "'.");
                    }
                    case "duplicate" -> {
                        ctx.require(3);
                        Iris.config().duplicate(ctx.arg(1), ctx.joinFrom(2));
                        ctx.ok("Duplicated '" + ctx.arg(1) + "' as '" + ctx.joinFrom(2) + "'.");
                    }
                    case "delete" -> {
                        ctx.require(2);
                        Iris.config().delete(ctx.joinFrom(1));
                        ctx.ok("Deleted '" + ctx.joinFrom(1) + "'.");
                    }
                    case "export" -> {
                        ctx.require(2);
                        Path dest = exportPath(ctx.has(2) ? ctx.arg(2) : ctx.arg(1), ".json");
                        Iris.config().exportTo(ctx.arg(1), dest);
                        ctx.ok("Exported to " + dest);
                    }
                    case "import" -> {
                        ctx.require(2);
                        String name = Iris.config().importFrom(exportPath(ctx.arg(1), ".json"), ctx.has(2) ? ctx.joinFrom(2) : null);
                        ctx.ok("Imported as '" + name + "'. Load it with config load.");
                    }
                    case "reset" -> {
                        Iris.config().applyDefaults();
                        ctx.ok("Active config reset to defaults.");
                    }
                    case "gui" -> Mc.openScreen(Screens.configs());
                    default -> throw new CommandException("Unknown subcommand '" + sub + "'.", true);
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                throw new CommandException(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) {
            if (i == 0) return List.of("list", "current", "save", "load", "create", "rename", "duplicate", "delete", "export", "import", "reset", "gui");
            String sub = ctx.args()[0].toLowerCase();
            if (i == 1 && List.of("load", "rename", "duplicate", "delete", "export").contains(sub)) return Iris.config().list();
            return List.of();
        }
    }
}
