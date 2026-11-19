package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.util.Format;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Health and hunger numbers. */
public final class HealthElement extends TextHudElement {
    private final BoolSetting hunger = add(new BoolSetting("Show hunger", "Also show the food level.", true));

    public HealthElement() {
        super("Health", "Shows health and hunger.", 0.01, 0.22, false);
    }

    @Override protected List<String> lines(boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        float hp = preview || mc.player == null ? 20f : mc.player.getHealth();
        int food = preview || mc.player == null ? 20 : mc.player.getFoodData().getFoodLevel();
        String s = "Health: " + Format.fixed(hp, 1);
        if (hunger.on()) s += "  Food: " + food;
        return List.of(s);
    }
}
