package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Current server address, or Singleplayer. */
public final class ServerElement extends TextHudElement {
    public ServerElement() {
        super("Server", "Shows the server you are connected to.", 0.01, 0.25, false);
    }

    @Override protected List<String> lines(boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        if (preview) return List.of("Server: play.example.net");
        if (mc.getCurrentServer() != null) return List.of("Server: " + mc.getCurrentServer().ip);
        return List.of("Server: " + (mc.getSingleplayerServer() != null ? "Singleplayer" : "-"));
    }
}
