package dev.iris.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.iris.client.core.Iris;
import dev.iris.client.gui.IrisScreen;
import dev.iris.client.gui.Screens;
import dev.iris.client.render.Anim;
import dev.iris.client.render.Draw;
import dev.iris.client.util.Input;
import dev.iris.client.util.Mc;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Fabric client entrypoint: wires Iris into the game loop, HUD layer, chat and the command dispatcher. */
public class IrisClient implements ClientModInitializer {
    private static boolean welcomeShown;
    private static boolean resendingChat;

    @Override
    public void onInitializeClient() {
        try {
            Iris.init();
        } catch (Throwable t) {
            // A startup failure must never prevent Minecraft from launching.
            Iris.LOGGER.error("Iris failed to initialize", t);
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Iris.MOD_ID, "overlay"), (graphics, tickCounter) -> onHud(graphics));

        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (!welcomeShown && screen instanceof TitleScreen) {
                welcomeShown = true;
                if (Iris.client().showWelcome.on()) Mc.openScreen(Screens.welcome());
            }
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(this::onOutgoingChat);
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || Iris.modules().allowIncomingChat(message.getString()));
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, timestamp) ->
                Iris.modules().allowIncomingChat(message.getString()));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("iris")
                        .executes(ctx -> {
                            Iris.commands().execute("help");
                            return 1;
                        })
                        .then(ClientCommands.argument("command", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> suggest(builder))
                                .executes(ctx -> {
                                    Iris.commands().execute(StringArgumentType.getString(ctx, "command"));
                                    return 1;
                                }))));

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            try {
                Iris.config().saveActive();
            } finally {
                Iris.modules().shutdown();
            }
        });
    }

    private void onTick(Minecraft mc) {
        try {
            Iris.modules().tick(mc);
            Iris.config().tick();
            if (mc.gui.screen() == null && mc.isWindowActive()) {
                if (Iris.client().openGuiKey.pressedEdge(Input.isKeyDown(Iris.client().openGuiKey.keyCode()))) {
                    Mc.openScreen(Screens.clickGui());
                }
                if (Iris.client().hudEditorKey.pressedEdge(Input.isKeyDown(Iris.client().hudEditorKey.keyCode()))) {
                    Mc.openScreen(Screens.hudEditor());
                }
            }
        } catch (Throwable t) {
            Iris.LOGGER.error("Iris tick failed", t);
        }
    }

    private void onHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        try {
            Anim.frame();
            Minecraft mc = Minecraft.getInstance();
            Draw draw = Draw.of(graphics);
            int sw = Mc.guiWidth();
            int sh = Mc.guiHeight();
            boolean hudSuppressed = mc.gui.screen() instanceof IrisScreen s && s.suppressesHud();
            if (!hudSuppressed) {
                Iris.hud().render(draw, sw, sh, false);
                Iris.modules().render(draw, 0f);
            }
            Iris.notifications().render(draw, sw, sh);
        } catch (Throwable t) {
            Iris.LOGGER.error("Iris HUD render failed", t);
        }
    }

    /** Handles the ".command" prefix and outgoing-chat module hooks. Returns false to cancel the message. */
    private boolean onOutgoingChat(String message) {
        if (resendingChat) return true;
        try {
            String prefix = Iris.client().prefix.get();
            if (!prefix.isEmpty() && message.startsWith(prefix)) {
                Iris.commands().execute(message.substring(prefix.length()));
                return false;
            }
            String out = Iris.modules().outgoingChat(message);
            if (out == null) return false;
            if (!out.equals(message)) {
                resendingChat = true;
                try {
                    Mc.sendChat(out);
                } finally {
                    resendingChat = false;
                }
                return false;
            }
        } catch (Throwable t) {
            Iris.LOGGER.error("Outgoing chat hook failed", t);
        }
        return true;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(SuggestionsBuilder builder) {
        String typed = builder.getRemaining();
        int lastSpace = typed.lastIndexOf(' ');
        SuggestionsBuilder sub = builder.createOffset(builder.getStart() + lastSpace + 1);
        List<String> options = Iris.commands().suggest(typed);
        String partial = sub.getRemainingLowerCase();
        for (String s : options) {
            if (s.toLowerCase().startsWith(partial)) sub.suggest(s.contains(" ") ? "\"" + s + "\"" : s);
        }
        return sub.buildFuture();
    }
}
