package dev.iris.client.module;

/** Module categories shown as tabs in the ClickGUI. */
public enum Category {
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World"),
    CHAT("Chat"),
    INFO("Info"),
    UTILITY("Utility"),
    CLIENT("Client");

    private final String displayName;

    Category(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    public static Category parse(String text) {
        for (Category c : values()) {
            if (c.displayName.equalsIgnoreCase(text) || c.name().equalsIgnoreCase(text)) return c;
        }
        return null;
    }
}
