package dev.iris.client.commands;

import dev.iris.client.command.Command;
import dev.iris.client.command.CommandCategory;
import dev.iris.client.command.CommandContext;
import dev.iris.client.command.CommandException;
import dev.iris.client.command.CommandManager;
import dev.iris.client.core.Iris;
import dev.iris.client.gui.Screens;
import dev.iris.client.hud.HudElement;
import dev.iris.client.setting.Setting;
import dev.iris.client.util.Format;
import dev.iris.client.util.Mc;

import java.util.ArrayList;
import java.util.List;

/** Commands for HUD elements and the HUD editor. */
public final class HudCommands {
    private HudCommands() {}

    public static void register(CommandManager cm) {
        cm.register(new Hud());
    }

    static final class Hud extends Command {
        Hud() {
            super("hud", CommandCategory.HUD, "Manages HUD elements.",
                    "<list|enable|disable|toggle|move|scale|set|reset|resetall|show|hide|editor> [args]");
        }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            String sub = ctx.arg(0).toLowerCase();
            switch (sub) {
                case "list" -> {
                    StringBuilder sb = new StringBuilder();
                    for (HudElement e : Iris.hud().all()) sb.append(sb.length() > 0 ? "§7, " : "").append(e.isEnabled() ? "§a" : "§f").append(e.name());
                    ctx.reply("HUD elements: " + sb);
                }
                case "enable", "disable", "toggle" -> {
                    ctx.require(2);
                    HudElement e = ctx.hudElement(1);
                    e.setEnabled(sub.equals("toggle") ? !e.isEnabled() : sub.equals("enable"));
                    ctx.ok(e.name() + (e.isEnabled() ? " shown." : " hidden."));
                }
                case "move" -> {
                    ctx.require(4);
                    HudElement e = ctx.hudElement(1);
                    e.setPosition(ctx.decimal(2, 0, 100) / 100.0, ctx.decimal(3, 0, 100) / 100.0);
                    ctx.ok(e.name() + " moved to " + Format.fixed(e.x() * 100, 1) + "%, " + Format.fixed(e.y() * 100, 1) + "%.");
                }
                case "scale" -> {
                    ctx.require(3);
                    HudElement e = ctx.hudElement(1);
                    e.setScale((float) ctx.decimal(2, HudElement.MIN_SCALE, HudElement.MAX_SCALE));
                    ctx.ok(e.name() + " scale " + e.scale() + ".");
                }
                case "set" -> {
                    ctx.require(4);
                    HudElement e = ctx.hudElement(1);
                    Setting<?> s = e.find(ctx.arg(2)).orElseThrow(() -> new CommandException("No setting '" + ctx.args()[2] + "' on " + e.name() + "."));
                    if (!s.parse(ctx.joinFrom(3))) throw new CommandException("'" + ctx.joinFrom(3) + "' is not valid for " + s.key() + ".");
                    ctx.ok(e.name() + " " + s.key() + " = " + s.display());
                    Iris.notifications().settingChanged(e.name(), s.name(), s.display());
                }
                case "reset" -> {
                    ctx.require(2);
                    HudElement e = ctx.hudElement(1);
                    e.resetAll();
                    ctx.ok(e.name() + " reset.");
                }
                case "resetall" -> {
                    Iris.hud().resetAll();
                    ctx.ok("All HUD elements reset.");
                }
                case "show", "hide" -> {
                    Iris.client().hudVisible.set(sub.equals("show"));
                    ctx.ok("HUD " + (sub.equals("show") ? "shown" : "hidden") + ".");
                }
                case "editor" -> Mc.openScreen(Screens.hudEditor());
                default -> throw new CommandException("Unknown subcommand '" + sub + "'.", true);
            }
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) {
            if (i == 0) return List.of("list", "enable", "disable", "toggle", "move", "scale", "set", "reset", "resetall", "show", "hide", "editor");
            List<String> names = new ArrayList<>();
            String sub = ctx.args()[0].toLowerCase();
            if (i == 1 && List.of("enable", "disable", "toggle", "move", "scale", "set", "reset").contains(sub)) {
                for (HudElement e : Iris.hud().all()) names.add(e.id());
            }
            if (i == 2 && sub.equals("set")) {
                HudElement e = Iris.hud().get(ctx.args()[1]);
                if (e != null) for (Setting<?> s : e.settings()) names.add(s.key());
            }
            return names;
        }
    }
}
