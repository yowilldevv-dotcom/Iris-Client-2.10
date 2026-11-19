package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ModeSetting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Real-world local time in 12h or 24h format. */
public final class ClockElement extends TextHudElement {
    private final ModeSetting format = add(new ModeSetting("Format", "12 or 24 hour clock.", "24h", "24h", "12h"));
    private final BoolSetting seconds = add(new BoolSetting("Seconds", "Show seconds.", false));
    private final BoolSetting date = add(new BoolSetting("Show date", "Show the date on a second line.", false));

    private String timePattern = "";
    private DateTimeFormatter timeFormatter;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    public ClockElement() {
        super("Clock", "Shows your computer's local time.", 0.16, 0.01, false);
    }

    @Override protected List<String> lines(boolean preview) {
        String pattern = (format.is("12h") ? "h:mm" : "HH:mm") + (seconds.on() ? ":ss" : "") + (format.is("12h") ? " a" : "");
        if (!pattern.equals(timePattern) || timeFormatter == null) {
            timePattern = pattern;
            timeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        }
        LocalDateTime now = LocalDateTime.now();
        List<String> out = new ArrayList<>(2);
        out.add("Time: " + timeFormatter.format(now));
        if (date.on()) out.add("Date: " + DATE.format(now));
        return out;
    }
}
