package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.util.Mc;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/** Latency to the server as reported in the player list. */
public final class PingElement extends TextHudElement {
    private final BoolSetting showLabel = add(new BoolSetting("Show label", "Prefix the value with 'Ping:'.", true));
    private final BoolSetting showUnit = add(new BoolSetting("Show unit", "Append 'ms'.", true));

    public PingElement() {
        super("Ping", "Shows your latency to the server.", 0.01, 0.13, false);
    }

    @Override protected List<String> lines(boolean preview) {
        String value = "42";
        if (!preview) {
            value = "n/a";
            LocalPlayer p = Mc.player();
            if (p != null && p.connection != null) {
                var info = p.connection.getPlayerInfo(p.getUUID());
                if (info != null) value = Integer.toString(info.getLatency());
            }
        }
        if (showUnit.on() && !value.equals("n/a")) value += " ms";
        return List.of(showLabel.on() ? "Ping: " + value : value);
    }
}
