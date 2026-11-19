package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.util.Format;
import dev.iris.client.util.Mc;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/** Name of the biome the player is standing in. */
public final class BiomeElement extends TextHudElement {
    private final BoolSetting showLabel = add(new BoolSetting("Show label", "Prefix the biome with 'Biome:'.", true));
    private final BoolSetting namespace = add(new BoolSetting("Show namespace", "Show the full id (e.g. minecraft:plains).", false));

    public BiomeElement() {
        super("Biome", "Shows the biome you are in.", 0.01, 0.37, false);
    }

    @Override protected List<String> lines(boolean preview) {
        String raw = "minecraft:plains";
        LocalPlayer p = Mc.player();
        var level = Mc.mc().level;
        if (!preview && p != null && level != null) {
            try {
                raw = level.getBiome(p.blockPosition()).getRegisteredName();
            } catch (RuntimeException e) {
                raw = "minecraft:unknown";
            }
        }
        String name = namespace.on() ? raw : Format.prettyId(raw);
        return List.of(showLabel.on() ? "Biome: " + name : name);
    }
}
