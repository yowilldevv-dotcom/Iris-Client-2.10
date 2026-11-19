package dev.iris.client.commands;

import dev.iris.client.command.Command;
import dev.iris.client.command.CommandCategory;
import dev.iris.client.command.CommandContext;
import dev.iris.client.command.CommandException;
import dev.iris.client.command.CommandManager;
import dev.iris.client.core.Iris;
import dev.iris.client.util.Format;
import dev.iris.client.util.Input;
import dev.iris.client.util.Mc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Read-only information commands about the game and the session. */
public final class InfoCommands {
    private static final long START = System.currentTimeMillis();

    private InfoCommands() {}

    public static void register(CommandManager cm) {
        cm.register(new Pos(), new Biome(), new Fps(), new Ping(), new Memory(), new Server(), new Session(),
                new Clock(), new Players(), new Stats(), new Facing(), new CopyPos());
    }

    private static LocalPlayer player() throws CommandException {
        LocalPlayer p = Mc.player();
        if (p == null) throw new CommandException("You are not in a world.");
        return p;
    }

    static final class Pos extends Command {
        Pos() { super("pos", CommandCategory.INFO, "Shows your coordinates and the Nether/Overworld equivalent.", "", "coords"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            LocalPlayer p = player();
            double x = p.getX(), y = p.getY(), z = p.getZ();
            ctx.reply("XYZ: §f" + Format.fixed(x, 1) + " " + Format.fixed(y, 1) + " " + Format.fixed(z, 1));
            ctx.line("  §7Other dimension: §f" + Format.fixed(x / 8, 1) + " / " + Format.fixed(x * 8, 1) + " (x ÷8 / ×8), z " + Format.fixed(z / 8, 1) + " / " + Format.fixed(z * 8, 1));
        }
    }

    static final class Biome extends Command {
        Biome() { super("biome", CommandCategory.INFO, "Shows the biome you are in.", ""); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            LocalPlayer p = player();
            ctx.reply("Biome: §f" + Format.prettyId(Minecraft.getInstance().level.getBiome(p.blockPosition()).getRegisteredName()));
        }
    }

    static final class Fps extends Command {
        Fps() { super("fps", CommandCategory.INFO, "Shows the current FPS.", ""); }
        @Override public void execute(CommandContext ctx) { ctx.reply("FPS: §f" + Minecraft.getInstance().getFps()); }
    }

    static final class Ping extends Command {
        Ping() { super("ping", CommandCategory.INFO, "Shows your latency to the server.", ""); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            LocalPlayer p = player();
            var info = p.connection == null ? null : p.connection.getPlayerInfo(p.getUUID());
            if (info == null) throw new CommandException("Ping is not available (singleplayer or not connected).");
            ctx.reply("Ping: §f" + info.getLatency() + " ms");
        }
    }

    static final class Memory extends Command {
        Memory() { super("memory", CommandCategory.INFO, "Shows Java memory usage.", "", "mem"); }

        @Override public void execute(CommandContext ctx) {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            ctx.reply("Memory: §f" + Format.bytes(used) + " §7used of §f" + Format.bytes(rt.maxMemory()));
        }
    }

    static final class Server extends Command {
        Server() { super("server", CommandCategory.INFO, "Shows the server you are connected to.", ""); }

        @Override public void execute(CommandContext ctx) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getCurrentServer() != null) ctx.reply("Server: §f" + mc.getCurrentServer().ip);
            else ctx.reply(mc.getSingleplayerServer() != null ? "You are in singleplayer." : "Not connected.");
        }
    }

    static final class Session extends Command {
        Session() { super("session", CommandCategory.INFO, "Shows how long this game session has run.", "", "uptime"); }
        @Override public void execute(CommandContext ctx) { ctx.reply("Session: §f" + Format.duration(System.currentTimeMillis() - START)); }
    }

    static final class Clock extends Command {
        Clock() { super("clock", CommandCategory.INFO, "Shows your real local time and the in-game time.", "", "time"); }

        @Override public void execute(CommandContext ctx) {
            ctx.reply("Real time: §f" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                ctx.line("  §7World age: §f" + mc.level.getGameTime() + " ticks");
                ctx.line("  §7Day/night clock: §funavailable through the Minecraft 26.2 client API");
            }
        }
    }

    static final class Players extends Command {
        Players() { super("players", CommandCategory.INFO, "Lists players in the loaded area.", "", "who"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            player();
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (var pl : Minecraft.getInstance().level.players()) {
                sb.append(n++ > 0 ? "§7, §f" : "§f").append(pl.getName().getString());
            }
            ctx.reply(n + " player(s) nearby: " + sb);
        }
    }

    static final class Stats extends Command {
        Stats() { super("stats", CommandCategory.INFO, "Shows how much content Iris has loaded.", ""); }

        @Override public void execute(CommandContext ctx) {
            int on = Iris.modules().enabled().length;
            ctx.reply(Iris.modules().count() + " modules (" + on + " enabled), " + Iris.hud().all().size() + " HUD elements, "
                    + Iris.commands().count() + " commands, " + Iris.themes().all().size() + " themes, config '" + Iris.config().activeName() + "'.");
        }
    }

    static final class Facing extends Command {
        Facing() { super("facing", CommandCategory.INFO, "Shows the direction you are facing.", "", "dir"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            LocalPlayer p = player();
            float yaw = ((p.getYRot() % 360) + 360) % 360;
            String[] names = {"South", "South-West", "West", "North-West", "North", "North-East", "East", "South-East"};
            ctx.reply("Facing: §f" + names[Math.round(yaw / 45f) % 8] + " §7(yaw " + Format.fixed(yaw, 1) + ", pitch " + Format.fixed(p.getXRot(), 1) + ")");
        }
    }

    static final class CopyPos extends Command {
        CopyPos() { super("copypos", CommandCategory.INFO, "Copies your block coordinates to the clipboard.", "", "cpos"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            LocalPlayer p = player();
            String s = p.blockPosition().getX() + " " + p.blockPosition().getY() + " " + p.blockPosition().getZ();
            Input.setClipboard(s);
            ctx.ok("Copied '" + s + "'.");
        }
    }
}
