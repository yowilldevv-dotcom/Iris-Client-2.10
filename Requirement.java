package dev.iris.client.command;

/** What must be true before a command may run. Checked centrally by the {@link CommandManager}. */
public enum Requirement {
    /** No requirement. */
    NONE,
    /** The player must be in a world (singleplayer or multiplayer). */
    IN_WORLD,
    /** The player must be in a singleplayer world (integrated server). */
    SINGLEPLAYER,
    /** In a world, and (where it can be determined) with operator permission. The server still has the final say. */
    OPERATOR
}
