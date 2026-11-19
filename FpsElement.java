package dev.iris.client.hud.elements;

import dev.iris.client.hud.TextHudElement;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.util.Format;

import java.util.List;

/** Frames per second, measured from the HUD's own frame cadence (no game internals needed). */
public final class FpsElement extends TextHudElement {
    private final BoolSetting showAverage = add(new BoolSetting("Show average", "Also show a smoothed average FPS.", false));
    private final BoolSetting showFrameTime = add(new BoolSetting("Show frame time", "Also show the frame time in milliseconds.", false));

    private int frames;
    private long windowStart = System.nanoTime();
    private double fps;
    private double avg;

    public FpsElement() {
        super("FPS", "Shows the current frame rate.", 0.01, 0.07, true);
    }

    @Override public void prepare(boolean preview) {
        frames++;
        long now = System.nanoTime();
        long dt = now - windowStart;
        if (dt > 2_000_000_000L) {
            // element was hidden for a while; restart the measurement window
            frames = 0;
            windowStart = now;
        } else if (dt >= 500_000_000L) {
            fps = frames * 1.0e9 / dt;
            avg = avg <= 0 ? fps : avg + (fps - avg) * 0.1;
            frames = 0;
            windowStart = now;
        }
        super.prepare(preview);
    }

    @Override protected List<String> lines(boolean preview) {
        double f = preview ? 144 : fps;
        double a = preview ? 139 : avg;
        if (f <= 0) return List.of("FPS: --");
        StringBuilder sb = new StringBuilder("FPS: ").append(Math.round(f));
        if (showAverage.on()) sb.append("  avg ").append(Math.round(a));
        if (showFrameTime.on()) sb.append("  ").append(Format.fixed(1000.0 / f, 1)).append(" ms");
        return List.of(sb.toString());
    }
}
