package dev.iris.client.command;

/** Thrown by commands for user errors (bad arguments, missing prerequisites). The message is shown to the user. */
public class CommandException extends Exception {
    private final boolean showUsage;

    public CommandException(String message) { this(message, false); }

    public CommandException(String message, boolean showUsage) {
        super(message);
        this.showUsage = showUsage;
    }

    public boolean showUsage() { return showUsage; }
}
