package dev.iris.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Small helpers around the Minecraft client. Version-sensitive calls used by many features live here so a
 * Minecraft API change is a one-file fix.
 */
public final class Mc {
    private Mc() {}

    public static Minecraft mc() { return Minecraft.getInstance(); }

    public static boolean inWorld() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    public static LocalPlayer player() { return Minecraft.getInstance().player; }

    public static boolean isSingleplayer() { return Minecraft.getInstance().getSingleplayerServer() != null; }

    /** Opens a screen (or closes the current one with null). */
    public static void openScreen(net.minecraft.client.gui.screens.Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(screen));
    }

    /** Shows a local chat line that only this client sees. */
    public static void chatLocal(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) mc.gui.hud.getChat().addMessage(Component.literal(text));
    }

    /** Sends a chat message or slash command to the server. A leading '/' is treated as a command. */
    public static void sendChat(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        if (text.startsWith("/")) mc.player.connection.sendCommand(text.substring(1));
        else mc.player.connection.sendChat(text);
    }

    /**
     * Best-effort operator check via reflection (the permission API changed across Minecraft versions). When it
     * cannot be determined this returns true and the server's own permission check decides.
     */
    public static boolean hasPermissionLevel(int level) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return false;
        try {
            java.lang.reflect.Method m = p.getClass().getMethod("hasPermissions", int.class);
            return (boolean) m.invoke(p, level);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }

    public static int guiWidth() { return Minecraft.getInstance().getWindow().getGuiScaledWidth(); }

    public static int guiHeight() { return Minecraft.getInstance().getWindow().getGuiScaledHeight(); }
}
