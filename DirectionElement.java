package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ModeSetting;
import dev.iris.client.util.Mc;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/** Facing direction as cardinal name, compass degrees (0 = north) or both. */
public final class DirectionElement extends TextHudElement {
    private static final String[] SHORT = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final String[] LONG = {"North", "North East", "East", "South East", "South", "South West", "West", "North West"};
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DZ = {-1, -1, 0, 1, 1, 1, 0, -1};

    private final ModeSetting display = add(new ModeSetting("Display", "What to show.", "Both", "Cardinal", "Degrees", "Both"));
    private final BoolSetting longNames = add(new BoolSetting("Long names", "Spell out directions (North East instead of NE).", false));
    private final BoolSetting axis = add(new BoolSetting("Show axis", "Show the world axis you are heading toward, e.g. (+X -Z).", false));

    public DirectionElement() {
        super("Direction", "Shows which way you are facing.", 0.01, 0.43, false);
    }

    @Override protected List<String> lines(boolean preview) {
        LocalPlayer p = Mc.player();
        float yaw = preview || p == null ? 135f : p.getYRot();
        float heading = (((yaw + 180f) % 360f) + 360f) % 360f; // 0 = north, clockwise
        int idx = Math.round(heading / 45f) % 8;
        String name = longNames.on() ? LONG[idx] : SHORT[idx];
        String deg = Math.round(heading) % 360 + "°";
        String text;
        if (display.is("Cardinal")) text = name;
        else if (display.is("Degrees")) text = deg;
        else text = name + " (" + deg + ")";
        if (axis.on() && (DX[idx] != 0 || DZ[idx] != 0)) {
            StringBuilder sb = new StringBuilder(text).append("  (");
            if (DX[idx] != 0) sb.append(DX[idx] > 0 ? "+X" : "-X");
            if (DX[idx] != 0 && DZ[idx] != 0) sb.append(' ');
            if (DZ[idx] != 0) sb.append(DZ[idx] > 0 ? "+Z" : "-Z");
            text = sb.append(')').toString();
        }
        return List.of("Facing: " + text);
    }
}
