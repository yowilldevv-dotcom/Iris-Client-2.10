package dev.iris.client.modules;

import dev.iris.client.module.ModuleManager;

/** Registers every built-in module. One static registrar per category keeps files small and focused. */
public final class ModuleRegistry {
    private ModuleRegistry() {}

    public static void registerAll(ModuleManager mm) {
        MovementModules.register(mm);
        RenderModules.register(mm);
        ChatModules.register(mm);
        PlayerModules.register(mm);
    }
}
