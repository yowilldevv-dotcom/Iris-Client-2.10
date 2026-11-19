package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ModeSetting;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.util.Format;
import dev.iris.client.util.Mc;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

/** Player coordinates with optional Nether/Overworld conversion lines. */
public final class CoordinatesElement extends TextHudElement {
    private final NumberSetting precision = add(new NumberSetting("Precision", "Decimal places (0 shows block coordinates).", 0, 0, 3, 1));
    private final BoolSetting singleLine = add(new BoolSetting("Single line", "Show X, Y and Z on one line.", false));
    private final ModeSetting convert = add(new ModeSetting("Conversion",
            "Show the matching coordinates in the other dimension (divide or multiply X and Z by 8).",
            "Off", "Off", "To Nether", "To Overworld", "Both"));

    public CoordinatesElement() {
        super("Coordinates", "Shows your X, Y and Z position.", 0.01, 0.19, true);
    }

    private String f(double v) {
        int p = precision.intValue();
        return p == 0 ? Long.toString((long) Math.floor(v)) : Format.fixed(v, p);
    }

    @Override protected List<String> lines(boolean preview) {
        LocalPlayer p = Mc.player();
        double x = 123.456, y = 64, z = -789.012;
        if (!preview && p != null) {
            x = p.getX();
            y = p.getY();
            z = p.getZ();
        }
        List<String> out = new ArrayList<>(5);
        if (singleLine.on()) {
            out.add("XYZ: " + f(x) + ", " + f(y) + ", " + f(z));
        } else {
            out.add("X: " + f(x));
            out.add("Y: " + f(y));
            out.add("Z: " + f(z));
        }
        if (convert.is("To Nether") || convert.is("Both")) out.add("Nether: " + f(x / 8.0) + ", " + f(z / 8.0));
        if (convert.is("To Overworld") || convert.is("Both")) out.add("Overworld: " + f(x * 8.0) + ", " + f(z * 8.0));
        return out;
    }

}
