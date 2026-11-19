package dev.iris.client.hud.elements;

import dev.iris.client.hud.HudManager;

/** Registers every built-in HUD element. */
public final class HudElements {
    private HudElements() {}

    public static void registerAll(HudManager hud) {
        hud.register(
                new WatermarkElement(), new FpsElement(), new CpsElement(), new SpeedElement(),
                new HealthElement(), new MemoryElement(), new ServerElement(),
                new CoordinatesElement(), new BiomeElement(), new DirectionElement(),
                new ClockElement(), new GameTimeElement(), new PingElement(),
                new ArmorStatusElement(), new KeystrokesElement(), new EnabledModulesElement());
    }
}
