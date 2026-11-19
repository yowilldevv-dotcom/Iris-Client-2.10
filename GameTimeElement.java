package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ModeSetting;
import dev.iris.client.util.Mc;

import java.util.List;
import java.util.Locale;

/** In-game time of day (a Minecraft day is 24000 ticks; tick 0 is 06:00). */
public final class GameTimeElement extends TextHudElement {
    private final ModeSetting display = add(new ModeSetting("Display", "What to show.", "Clock", "Clock", "Ticks", "Phase", "Clock + Phase"));
    private final BoolSetting hours24 = add(new BoolSetting("24 hour", "Use a 24 hour clock.", true));

    public GameTimeElement() {
        super("Game Time", "Shows the time of day in the Minecraft world.", 0.16, 0.07, false);
    }

    @Override protected List<String> lines(boolean preview) {
        long t = 6000;
        var level = Mc.mc().level;
        if (!preview && level != null) t = level.getGameTime();

        int hour = (int) ((t / 1000 + 6) % 24);
        int minute = (int) ((t % 1000) * 60 / 1000);
        String clock;
        if (hours24.on()) {
            clock = String.format(Locale.ROOT, "%02d:%02d", hour, minute);
        } else {
            int h12 = hour % 12 == 0 ? 12 : hour % 12;
            clock = String.format(Locale.ROOT, "%d:%02d %s", h12, minute, hour < 12 ? "AM" : "PM");
        }
        String phase;
        if (t < 12000) phase = "Day";
        else if (t < 13000) phase = "Sunset";
        else if (t < 23000) phase = "Night";
        else phase = "Sunrise";

        String text;
        if (preview) {
            if (display.is("Ticks")) text = t + " ticks";
            else if (display.is("Phase")) text = phase;
            else if (display.is("Clock + Phase")) text = clock + " (" + phase + ")";
            else text = clock;
            return List.of("World time: " + text);
        }

        // Minecraft 26.2 replaced the old day-time counter with world clocks.
        // The old ClientLevel#getDayTime() API no longer exists, so do not
        // mislabel elapsed game ticks as the time of day.
        if (display.is("Ticks")) return List.of("World age: " + t + " ticks");
        return List.of("World clock: unavailable");
    }
}
