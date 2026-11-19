package dev.iris.client.commands;

import dev.iris.client.command.CommandManager;

/** Registers every built-in command. One static registrar per area keeps files small and focused. */
public final class CommandRegistry {
    private CommandRegistry() {}

    public static void registerAll(CommandManager cm) {
        ModuleCommands.register(cm);
        ConfigCommands.register(cm);
        ThemeCommands.register(cm);
        HudCommands.register(cm);
        ClientCommands.register(cm);
        InfoCommands.register(cm);
        UtilityCommands.register(cm);
    }
}
