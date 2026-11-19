package dev.iris.client.commands;

import dev.iris.client.command.Command;
import dev.iris.client.command.CommandCategory;
import dev.iris.client.command.CommandContext;
import dev.iris.client.command.CommandException;
import dev.iris.client.command.CommandManager;
import dev.iris.client.command.Requirement;
import dev.iris.client.util.Format;
import dev.iris.client.util.Input;
import dev.iris.client.util.Mc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** General purpose helper commands. */
public final class UtilityCommands {
    private static long stopwatchStart = -1;

    private UtilityCommands() {}

    public static void register(CommandManager cm) {
        cm.register(new Say(), new Calc(), new Copy(), new Rand(), new Dice(), new Coinflip(), new RandUuid(),
                new Echo(), new B64(), new Sha(), new Stopwatch());
    }

    static final class Say extends Command {
        Say() { super("say", CommandCategory.UTILITY, "Sends a chat message or /command to the server.", "<text>", "chat"); }

        @Override public Requirement requirement() { return Requirement.IN_WORLD; }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            Mc.sendChat(ctx.joinFrom(0));
        }
    }

    static final class Echo extends Command {
        Echo() { super("echo", CommandCategory.UTILITY, "Prints text to your own chat.", "<text>"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            ctx.line(ctx.joinFrom(0));
        }
    }

    static final class Copy extends Command {
        Copy() { super("copy", CommandCategory.UTILITY, "Copies text to the clipboard.", "<text>"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            Input.setClipboard(ctx.joinFrom(0));
            ctx.ok("Copied " + ctx.joinFrom(0).length() + " characters.");
        }
    }

    static final class Rand extends Command {
        Rand() { super("rand", CommandCategory.UTILITY, "Random whole number in a range.", "<min> <max>", "random"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(2);
            int a = ctx.integer(0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
            int b = ctx.integer(1, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
            if (a > b) throw new CommandException("min must be <= max.");
            ctx.reply("Random: §f" + ThreadLocalRandom.current().nextInt(a, b + 1));
        }
    }

    static final class Dice extends Command {
        Dice() { super("dice", CommandCategory.UTILITY, "Rolls dice (default one d6).", "[count] [sides]", "roll"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            int n = ctx.has(0) ? ctx.integer(0, 1, 20) : 1;
            int sides = ctx.has(1) ? ctx.integer(1, 2, 1000) : 6;
            StringBuilder sb = new StringBuilder();
            int sum = 0;
            for (int i = 0; i < n; i++) {
                int r = ThreadLocalRandom.current().nextInt(1, sides + 1);
                sum += r;
                sb.append(i > 0 ? ", " : "").append(r);
            }
            ctx.reply("Rolled " + n + "d" + sides + ": §f" + sb + (n > 1 ? " §7(total " + sum + ")" : ""));
        }
    }

    static final class Coinflip extends Command {
        Coinflip() { super("coinflip", CommandCategory.UTILITY, "Flips a coin.", "", "flip"); }
        @Override public void execute(CommandContext ctx) { ctx.reply(ThreadLocalRandom.current().nextBoolean() ? "§fHeads" : "§fTails"); }
    }

    static final class RandUuid extends Command {
        RandUuid() { super("randuuid", CommandCategory.UTILITY, "Generates a random UUID and copies it.", ""); }

        @Override public void execute(CommandContext ctx) {
            String u = UUID.randomUUID().toString();
            Input.setClipboard(u);
            ctx.reply("UUID: §f" + u + " §7(copied)");
        }
    }

    static final class B64 extends Command {
        B64() { super("base64", CommandCategory.UTILITY, "Encodes or decodes Base64 text.", "<encode|decode> <text>", "b64"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(2);
            String text = ctx.joinFrom(1);
            switch (ctx.arg(0).toLowerCase()) {
                case "encode" -> ctx.reply("§f" + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
                case "decode" -> {
                    try {
                        ctx.reply("§f" + new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8));
                    } catch (IllegalArgumentException e) {
                        throw new CommandException("That is not valid Base64.");
                    }
                }
                default -> throw new CommandException("Use encode or decode.", true);
            }
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? List.of("encode", "decode") : List.of(); }
    }

    static final class Sha extends Command {
        Sha() { super("sha256", CommandCategory.UTILITY, "Computes the SHA-256 hash of text.", "<text>", "hash"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            try {
                byte[] h = MessageDigest.getInstance("SHA-256").digest(ctx.joinFrom(0).getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : h) sb.append(String.format("%02x", b));
                ctx.reply("§f" + sb);
            } catch (NoSuchAlgorithmException e) {
                throw new CommandException("SHA-256 is not available on this Java runtime.");
            }
        }
    }

    static final class Stopwatch extends Command {
        Stopwatch() { super("stopwatch", CommandCategory.UTILITY, "Starts, checks or stops a stopwatch.", "<start|check|stop>", "sw"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            long now = System.currentTimeMillis();
            switch (ctx.arg(0).toLowerCase()) {
                case "start" -> { stopwatchStart = now; ctx.ok("Stopwatch started."); }
                case "check" -> {
                    if (stopwatchStart < 0) throw new CommandException("Stopwatch is not running.");
                    ctx.reply("Elapsed: §f" + Format.duration(now - stopwatchStart));
                }
                case "stop" -> {
                    if (stopwatchStart < 0) throw new CommandException("Stopwatch is not running.");
                    ctx.reply("Stopped at §f" + Format.duration(now - stopwatchStart));
                    stopwatchStart = -1;
                }
                default -> throw new CommandException("Use start, check or stop.", true);
            }
        }

        @Override public List<String> suggest(CommandContext ctx, int i, String p) { return i == 0 ? List.of("start", "check", "stop") : List.of(); }
    }

    // ---- calculator: recursive descent, no eval ----

    static final class Calc extends Command {
        Calc() { super("calc", CommandCategory.UTILITY, "Evaluates + - * / ^ % and parentheses.", "<expression>", "math"); }

        @Override public void execute(CommandContext ctx) throws CommandException {
            ctx.require(1);
            String expr = ctx.joinFrom(0).replace(" ", "");
            Parser p = new Parser(expr);
            double v = p.expression();
            if (p.pos != expr.length()) throw new CommandException("Unexpected '" + expr.charAt(p.pos) + "' at position " + (p.pos + 1) + ".");
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new CommandException("Result is not a finite number.");
            ctx.reply(ctx.joinFrom(0) + " = §f" + (v == Math.rint(v) && Math.abs(v) < 1e15 ? Long.toString((long) v) : Double.toString(v)));
        }

        private static final class Parser {
            final String s;
            int pos;
            int depth;

            Parser(String s) { this.s = s; }

            double expression() throws CommandException {
                double v = term();
                while (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                    char op = s.charAt(pos++);
                    double r = term();
                    v = op == '+' ? v + r : v - r;
                }
                return v;
            }

            double term() throws CommandException {
                double v = power();
                while (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/' || s.charAt(pos) == '%')) {
                    char op = s.charAt(pos++);
                    double r = power();
                    if (op == '*') v *= r;
                    else if (r == 0) throw new CommandException("Division by zero.");
                    else v = op == '/' ? v / r : v % r;
                }
                return v;
            }

            double power() throws CommandException {
                double base = unary();
                if (pos < s.length() && s.charAt(pos) == '^') {
                    pos++;
                    return Math.pow(base, power());
                }
                return base;
            }

            double unary() throws CommandException {
                if (pos < s.length() && s.charAt(pos) == '-') { pos++; return -unary(); }
                if (pos < s.length() && s.charAt(pos) == '+') { pos++; return unary(); }
                return atom();
            }

            double atom() throws CommandException {
                if (pos >= s.length()) throw new CommandException("Expression ended unexpectedly.");
                if (s.charAt(pos) == '(') {
                    if (++depth > 50) throw new CommandException("Too many nested parentheses.");
                    pos++;
                    double v = expression();
                    if (pos >= s.length() || s.charAt(pos) != ')') throw new CommandException("Missing ')'.");
                    pos++;
                    depth--;
                    return v;
                }
                int start = pos;
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
                if (start == pos) throw new CommandException("Unexpected '" + s.charAt(pos) + "' at position " + (pos + 1) + ".");
                try {
                    return Double.parseDouble(s.substring(start, pos));
                } catch (NumberFormatException e) {
                    throw new CommandException("'" + s.substring(start, pos) + "' is not a number.");
                }
            }
        }
    }
}
