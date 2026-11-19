package dev.iris.client.core;

import dev.iris.client.render.Anim;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.KeyNames;
import dev.iris.client.setting.KeybindSetting;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.setting.SettingGroup;
import dev.iris.client.setting.TextSetting;

/** Global client preferences (saved in every configuration). */
public final class ClientPrefs extends SettingGroup {
    public final TextSetting prefix = add(new TextSetting("Prefix", "Chat prefix for Iris commands.", ".", 2));
    public final KeybindSetting openGuiKey = add(new KeybindSetting("ClickGUI key", "Key that opens the ClickGUI.", KeyNames.parse("RSHIFT")));
    public final KeybindSetting hudEditorKey = add(new KeybindSetting("HUD editor key", "Key that opens the HUD editor.", KeyNames.NONE));
    public final BoolSetting autosave = add(new BoolSetting("Autosave", "Automatically save changes to the active config.", true));
    public final BoolSetting hudVisible = add(new BoolSetting("HUD visible", "Master switch for all HUD elements.", true));
    public final BoolSetting showWelcome = add(new BoolSetting("Welcome screen", "Show the Iris welcome screen at startup.", true));
    public final BoolSetting animations = add(new BoolSetting("Animations", "Enable GUI and notification animations.", true));
    public final NumberSetting animationSpeed = add(new NumberSetting("Animation speed", "Speed multiplier for animations.", 1.0, 0.5, 3.0, 0.1));
    public final NumberSetting guiScale = add(new NumberSetting("GUI scale", "Scale of Iris windows.", 1.0, 0.75, 1.5, 0.05));
    public final BoolSetting chatFeedback = add(new BoolSetting("Chat feedback", "Print command results in chat.", true));

    public ClientPrefs() {
        super("Client", "Global Iris preferences.");
        animations.onChange(v -> syncAnimations());
        animationSpeed.onChange(v -> syncAnimations());
    }

    public void syncAnimations() {
        Anim.configure(animations.on(), animationSpeed.flt());
    }
}
