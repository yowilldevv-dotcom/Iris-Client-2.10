package dev.iris.client.command;

import dev.iris.client.core.Iris;
import dev.iris.client.util.Mc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Registry, parser, executor and tab-completion provider for Iris commands. */
public final class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private final Map<String, Command> byName = new HashMap<>();

    public void register(Command... list) {
        for (Command c : list) {
            put(c.name(), c);
            for (String a : c.aliases()) put(a, c);
            commands.add(c);
        }
        commands.sort(Comparator.comparing(Command::name));
    }

    private void put(String key, Command c) {
        String k = key.toLowerCase(Locale.ROOT);
        if (byName.putIfAbsent(k, c) != null) throw new IllegalStateException("Duplicate command name/alias: " + key);
    }

    public List<Command> all() { return List.copyOf(commands); }

    public int count() { return commands.size(); }

    public Command find(String nameOrAlias) {
        return nameOrAlias == null ? null : byName.get(nameOrAlias.toLowerCase(Locale.ROOT));
    }

    public List<Command> byCategory(CommandCategory category) {
        List<Command> out = new ArrayList<>();
        for (Command c : commands) if (c.category() == category) out.add(c);
        return out;
    }

    // ---- parsing ----

    /** Splits on spaces; double quotes group words ("my config" is one argument). */
    public static List<String> tokenize(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        boolean has = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
                has = true;
            } else if (ch == ' ' && !quoted) {
                if (has) { out.add(cur.toString()); cur.setLength(0); has = false; }
            } else {
                cur.append(ch);
                has = true;
            }
        }
        if (has) out.add(cur.toString());
        return out;
    }

    // ---- execution ----

    /** Runs a command line (without the prefix). Always reports the outcome to the user; never throws. */
    public void execute(String line) {
        List<String> tokens = tokenize(line.trim());
        if (tokens.isEmpty()) {
            Command help = find("help");
            if (help != null) runSafely(help, new String[0], "help");
            return;
        }
        String label = tokens.get(0);
        Command cmd = find(label);
        if (cmd == null) {
            String near = closest(label);
            Mc.chatLocal("§8[§bIris§8]§r §cUnknown command '" + label + "'."
                    + (near != null ? " Did you mean §f" + near + "§c?" : " Try §f" + CommandContext.prefix() + "help§c."));
            Iris.notifications().commandResult(label, false, "Unknown command");
            return;
        }
        runSafely(cmd, tokens.subList(1, tokens.size()).toArray(new String[0]), label);
    }

    private void runSafely(Command cmd, String[] args, String label) {
        CommandContext ctx = new CommandContext(cmd, args);
        String denied = checkRequirement(cmd.requirement());
        if (denied != null) {
            ctx.error(denied);
            Iris.notifications().commandResult(cmd.name(), false, denied);
            return;
        }
        try {
            cmd.execute(ctx);
            Iris.notifications().commandResult(cmd.name(), true, "Done");
        } catch (CommandException e) {
            ctx.error(e.getMessage());
            if (e.showUsage()) ctx.reply("§7Usage: §f" + CommandContext.prefix() + cmd.name() + " " + cmd.usage());
            Iris.notifications().commandResult(cmd.name(), false, e.getMessage());
        } catch (RuntimeException e) {
            Iris.LOGGER.error("Command '{}' crashed", cmd.name(), e);
            ctx.error("Command failed: " + e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : ""));
            Iris.notifications().error("Command error", cmd.name() + " failed");
        }
    }

    private String checkRequirement(Requirement r) {
        switch (r) {
            case NONE:
                return null;
            case IN_WORLD:
                return Mc.inWorld() ? null : "This command only works while you are in a world.";
            case SINGLEPLAYER:
                if (!Mc.inWorld()) return "This command only works while you are in a world.";
                return Mc.isSingleplayer() ? null : "This command only works in singleplayer.";
            case OPERATOR:
                if (!Mc.inWorld()) return "This command only works while you are in a world.";
                return Mc.hasPermissionLevel(2) ? null : "You need operator permission (level 2) for this command.";
            default:
                return null;
        }
    }

    // ---- completion ----

    /**
     * Completion candidates for the text typed after the prefix (or after "/iris ").
     * Returns candidates for the last (possibly empty) token only.
     */
    public List<String> suggest(String typed) {
        boolean trailingSpace = typed.endsWith(" ");
        List<String> tokens = tokenize(typed);
        if (tokens.isEmpty() || (tokens.size() == 1 && !trailingSpace)) {
            String partial = tokens.isEmpty() ? "" : tokens.get(0);
            Set<String> out = new LinkedHashSet<>();
            for (Command c : commands) {
                if (c.name().startsWith(partial.toLowerCase(Locale.ROOT))) out.add(c.name());
                for (String a : c.aliases()) if (a.startsWith(partial.toLowerCase(Locale.ROOT))) out.add(a);
            }
            return new ArrayList<>(out);
        }
        Command cmd = find(tokens.get(0));
        if (cmd == null) return List.of();
        int argIndex = trailingSpace ? tokens.size() - 1 : tokens.size() - 2;
        String partial = trailingSpace ? "" : tokens.get(tokens.size() - 1);
        List<String> done = tokens.subList(1, Math.min(tokens.size(), 1 + argIndex));
        try {
            List<String> raw = cmd.suggest(new CommandContext(cmd, done.toArray(new String[0])), argIndex, partial);
            return CommandContext.startingWith(raw, partial);
        } catch (RuntimeException e) {
            Iris.LOGGER.debug("Suggestion provider for {} failed", cmd.name(), e);
            return List.of();
        }
    }

    /** Closest command name by edit distance (<= 2), or null. */
    public String closest(String input) {
        String in = input.toLowerCase(Locale.ROOT);
        String best = null;
        int bestDist = 3;
        for (String name : byName.keySet()) {
            int d = distance(in, name);
            if (d < bestDist) { bestDist = d; best = name; }
        }
        return best;
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
