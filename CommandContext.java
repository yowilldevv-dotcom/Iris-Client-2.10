package dev.iris.client.command;

import dev.iris.client.core.Iris;
import dev.iris.client.hud.HudElement;
import dev.iris.client.module.Module;
import dev.iris.client.theme.Theme;
import dev.iris.client.util.Mc;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Parsed invocation passed to {@link Command#execute}. Parsing helpers throw {@link CommandException} with a clear
 * message, so command bodies stay short and every error is reported consistently.
 */
public final class CommandContext {
    private final Command command;
    private final String[] args;

    public CommandContext(Command command, String[] args) {
        this.command = command;
        this.args = args;
    }

    public Command command() { return command; }
    public String[] args() { return args; }
    public int size() { return args.length; }
    public boolean has(int i) { return i < args.length; }

    public String arg(int i) throws CommandException {
        if (i >= args.length) throw new CommandException("Missing argument <" + i + ">.", true);
        return args[i];
    }

    public String argOr(int i, String fallback) { return i < args.length ? args[i] : fallback; }

    /** Requires at least {@code n} arguments, otherwise shows usage. */
    public void require(int n) throws CommandException {
        if (args.length < n) throw new CommandException("Not enough arguments.", true);
    }

    /** Joins arguments from index {@code from} to the end with spaces. */
    public String joinFrom(int from) {
        if (from >= args.length) return "";
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    public int integer(int i, int min, int max) throws CommandException {
        String s = arg(i);
        try {
            int v = Integer.parseInt(s);
            if (v < min || v > max) throw new CommandException("'" + s + "' must be between " + min + " and " + max + ".");
            return v;
        } catch (NumberFormatException e) {
            throw new CommandException("'" + s + "' is not a whole number.");
        }
    }

    public double decimal(int i, double min, double max) throws CommandException {
        String s = arg(i);
        try {
            double v = Double.parseDouble(s);
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new NumberFormatException();
            if (v < min || v > max) throw new CommandException("'" + s + "' must be between " + min + " and " + max + ".");
            return v;
        } catch (NumberFormatException e) {
            throw new CommandException("'" + s + "' is not a number.");
        }
    }

    public boolean bool(int i) throws CommandException {
        String s = arg(i).toLowerCase(Locale.ROOT);
        return switch (s) {
            case "true", "on", "yes", "1", "enable" -> true;
            case "false", "off", "no", "0", "disable" -> false;
            default -> throw new CommandException("'" + s + "' is not on/off.");
        };
    }

    public Module module(int i) throws CommandException {
        String name = arg(i);
        Module m = Iris.modules().get(name);
        if (m == null) throw new CommandException("Unknown module '" + name + "'. Try " + prefix() + "modules.");
        return m;
    }

    public HudElement hudElement(int i) throws CommandException {
        String name = arg(i);
        HudElement e = Iris.hud().get(name);
        if (e == null) throw new CommandException("Unknown HUD element '" + name + "'. Try " + prefix() + "hud list.");
        return e;
    }

    public Theme theme(int i) throws CommandException {
        String name = arg(i);
        Theme t = Iris.themes().get(name);
        if (t == null) throw new CommandException("Unknown theme '" + name + "'. Try " + prefix() + "theme list.");
        return t;
    }

    public static String prefix() { return Iris.client().prefix.get(); }

    // ---- output ----

    /** Plain feedback line (§ color codes allowed). */
    public void reply(String message) { Mc.chatLocal("§8[§bIris§8]§r " + message); }

    public void ok(String message) { reply("§a" + message); }

    public void warn(String message) { reply("§e" + message); }

    public void error(String message) { reply("§c" + message); }

    /** Raw line without the Iris tag (for list output). */
    public void line(String message) { Mc.chatLocal(message); }

    public static List<String> startingWith(List<String> options, String partial) {
        String p = partial.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
