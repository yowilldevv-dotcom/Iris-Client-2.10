package dev.iris.client.notify;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Anim;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.theme.Theme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Toast-style notifications. Drawn by the HUD layer in-game and by Iris screens elsewhere; a per-frame guard
 * prevents drawing twice when both are active. Repeated identical notifications are merged to stay unobtrusive.
 */
public final class NotificationManager {
    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    private static final int MAX_QUEUE = 24;
    private static final long MERGE_WINDOW_MS = 600;

    private static final class Toast {
        final Type type;
        final String title;
        final String message;
        long created;
        long lifeMs;
        int count = 1;

        Toast(Type type, String title, String message, long lifeMs) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.created = System.currentTimeMillis();
            this.lifeMs = lifeMs;
        }
    }

    private final NotificationSettings settings;
    private final List<Toast> toasts = new ArrayList<>();
    private long lastRenderNanos;

    public NotificationManager(NotificationSettings settings) { this.settings = settings; }

    public NotificationSettings settings() { return settings; }

    // ---- posting ----

    public void post(Type type, String title, String message) {
        if (!settings.enabled.on()) return;
        long life = Math.round(settings.duration.dbl() * 1000);
        long now = System.currentTimeMillis();
        for (Toast t : toasts) {
            if (t.type == type && t.title.equals(title) && t.message.equals(message) && now - t.created < MERGE_WINDOW_MS) {
                t.count++;
                t.created = now;
                return;
            }
        }
        toasts.add(new Toast(type, title, message, life));
        while (toasts.size() > MAX_QUEUE) toasts.remove(0);
    }

    public void info(String title, String message) { post(Type.INFO, title, message); }
    public void success(String title, String message) { post(Type.SUCCESS, title, message); }
    public void warn(String title, String message) { post(Type.WARNING, title, message); }

    public void error(String title, String message) {
        if (settings.onErrors.on()) post(Type.ERROR, title, message);
    }

    public void moduleToggled(String name, boolean enabled) {
        if (settings.onModules.on()) post(enabled ? Type.SUCCESS : Type.INFO, name, enabled ? "Enabled" : "Disabled");
    }

    public void configEvent(String title, String message) {
        if (settings.onConfigs.on()) post(Type.INFO, title, message);
    }

    public void settingChanged(String owner, String setting, String value) {
        if (settings.onSettings.on()) post(Type.INFO, owner, setting + " → " + value);
    }

    public void commandResult(String command, boolean ok, String message) {
        if (settings.onCommands.on()) post(ok ? Type.SUCCESS : Type.WARNING, "." + command, message);
    }

    public void clear() { toasts.clear(); }

    public int activeCount() { return toasts.size(); }

    // ---- rendering ----

    public void render(Draw d, int screenW, int screenH) {
        long nowN = System.nanoTime();
        if (nowN - lastRenderNanos < 1_000_000L) return; // already drawn this frame
        lastRenderNanos = nowN;
        if (toasts.isEmpty() || !settings.enabled.on()) return;

        long now = System.currentTimeMillis();
        Theme theme = Iris.themes().current();
        int max = settings.maxVisible.intValue();
        float scale = settings.scale.flt();
        boolean compact = settings.compact.on();
        String pos = settings.position.get();
        boolean top = pos.startsWith("Top");
        boolean left = pos.endsWith("Left");
        boolean center = pos.endsWith("Center");

        float w = 170 * scale;
        float h = (compact ? 20 : 32) * scale;
        float gap = 4 * scale;
        float margin = 8;

        // Drop expired toasts first.
        for (Iterator<Toast> it = toasts.iterator(); it.hasNext(); ) {
            Toast t = it.next();
            if (now - t.created > t.lifeMs) it.remove();
        }

        int shown = 0;
        float cursor = margin;
        for (Toast t : toasts) {
            if (shown >= max) break;
            float age = (now - t.created) / 1000f;
            float life = t.lifeMs / 1000f;
            float in = Anim.enabled() ? Math.min(1f, age / 0.22f) : 1f;
            float out = Anim.enabled() ? Math.min(1f, (life - age) / 0.28f) : 1f;
            float p = Anim.easeOutCubic(Math.max(0f, Math.min(in, out)));

            String anim = settings.animation.get();
            float alpha = anim.equals("None") ? 1f : (anim.equals("Slide") ? Math.min(1f, p * 1.5f) : p);
            if (alpha <= 0.01f) { shown++; cursor += h + gap; continue; }

            float baseX = center ? (screenW - w) / 2f : (left ? margin : screenW - w - margin);
            float baseY = top ? cursor : screenH - cursor - h;
            float x = baseX;
            float y = baseY;
            if (anim.equals("Slide")) {
                float dist = (w + margin) * (1f - p);
                if (center) y += (top ? -1 : 1) * (h + margin) * (1f - p);
                else x += left ? -dist : dist;
            } else if (anim.equals("Pop")) {
                y += (top ? -1 : 1) * 6 * (1f - p);
            }

            drawToast(d, theme, t, x, y, w, h, alpha, scale, compact, age / life);
            cursor += h + gap;
            shown++;
        }
    }

    private void drawToast(Draw d, Theme theme, Toast t, float x, float y, float w, float h, float alpha,
                           float scale, boolean compact, float progress) {
        int accent = switch (t.type) {
            case SUCCESS -> theme.success();
            case WARNING -> theme.warning();
            case ERROR -> theme.error();
            case INFO -> theme.accent();
        };
        float bgA = settings.opacity.flt() * alpha;
        d.rrect(x, y, w, h, 3, Colors.mulAlpha(theme.panel(), bgA));
        d.rect(x, y + 2, 2 * scale, h - 4, Colors.mulAlpha(accent, alpha));
        float tx = x + 7 * scale;
        String title = t.count > 1 ? t.title + " ×" + t.count : t.title;
        if (compact) {
            d.pushScaled(tx, y + (h - d.lineHeight() * scale) / 2f, scale);
            d.textFit(title + "  " + t.message, 0, 0, (w - 12 * scale) / scale, Colors.mulAlpha(theme.text(), alpha), false);
            d.pop();
        } else {
            d.pushScaled(tx, y + 5 * scale, scale);
            d.textFit(title, 0, 0, (w - 12 * scale) / scale, Colors.mulAlpha(accent, alpha), false);
            d.textFit(t.message, 0, d.lineHeight() + 2, (w - 12 * scale) / scale, Colors.mulAlpha(theme.textDim(), alpha), false);
            d.pop();
        }
        if (settings.progressBar.on()) {
            float remaining = Math.max(0f, 1f - progress);
            d.rect(x + 2, y + h - 2 * scale, (w - 4) * remaining, Math.max(1, 1.5f * scale), Colors.mulAlpha(accent, alpha * 0.8f));
        }
    }
}
