package dev.iris.client.command;

import java.util.List;

/**
 * A client-side command. Run as {@code .name args} in chat or {@code /iris name args} (with tab completion).
 *
 * <p>Rules for implementations: perform the advertised action (no message-only stubs), validate arguments through
 * {@link CommandContext}, throw {@link CommandException} for user errors, and use {@link #requirement()} for
 * permission/precondition checks so the manager enforces them uniformly.
 */
public abstract class Command {
    private final String name;
    private final String[] aliases;
    private final CommandCategory category;
    private final String description;
    private final String usage;

    protected Command(String name, CommandCategory category, String description, String usage, String... aliases) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.usage = usage;
        this.aliases = aliases;
    }

    public String name() { return name; }
    public String[] aliases() { return aliases; }
    public CommandCategory category() { return category; }
    public String description() { return description; }

    /** Arguments only, e.g. "<module> [on|off]". Shown as ".name <module> [on|off]". */
    public String usage() { return usage; }

    public Requirement requirement() { return Requirement.NONE; }

    public abstract void execute(CommandContext ctx) throws CommandException;

    /**
     * Completion candidates for the argument currently being typed.
     *
     * @param ctx        completed arguments before the cursor
     * @param argIndex   index of the argument being completed (0 = first argument)
     * @param partial    text typed so far for that argument
     */
    public List<String> suggest(CommandContext ctx, int argIndex, String partial) { return List.of(); }
}
