package dev.iris.client.command;

/** Groups commands in the help output. */
public enum CommandCategory {
    MODULES("Modules"),
    CONFIG("Config"),
    THEME("Theme"),
    HUD("HUD"),
    CLIENT("Client"),
    PLAYER("Player"),
    WORLD("World"),
    INFO("Info"),
    UTILITY("Utility");

    private final String displayName;

    CommandCategory(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    public static CommandCategory parse(String text) {
        for (CommandCategory c : values()) {
            if (c.displayName.equalsIgnoreCase(text) || c.name().equalsIgnoreCase(text)) return c;
        }
        return null;
    }
}
