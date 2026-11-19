package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.util.Format;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Horizontal movement speed in blocks per second, smoothed. */
public final class SpeedElement extends TextHudElement {
    private final NumberSetting precision = add(new NumberSetting("Precision", "Decimal places.", 1, 0, 3, 1));
    private double smoothed;

    public SpeedElement() {
        super("Speed", "Shows your horizontal speed in blocks per second.", 0.01, 0.16, false);
    }

    @Override protected List<String> lines(boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        double bps = 4.3;
        if (!preview && mc.player != null) {
            Vec3 v = mc.player.getDeltaMovement();
            bps = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
        }
        smoothed += (bps - smoothed) * 0.2;
        return List.of("Speed: " + Format.fixed(smoothed, precision.intValue()) + " b/s");
    }
}
