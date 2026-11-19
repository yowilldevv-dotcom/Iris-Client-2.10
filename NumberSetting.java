package dev.iris.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** Numeric slider setting. Values are clamped to [min, max] and snapped to {@code step}. */
public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, String description, double def, double min, double max, double step) {
        super(name, description, def);
        if (min > max || step <= 0) throw new IllegalArgumentException("bad range for " + name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = sanitize(def);
    }

    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }
    public boolean isInteger() { return step >= 1 && step == Math.floor(step); }

    public double dbl() { return value; }
    public float flt() { return value.floatValue(); }
    public int intValue() { return (int) Math.round(value); }

    /** 0..1 position of the current value within the range (for sliders). */
    public double fraction() { return max == min ? 0 : (value - min) / (max - min); }

    public void setFraction(double f) { set(min + Math.max(0, Math.min(1, f)) * (max - min)); }

    @Override protected Double sanitize(Double candidate) {
        double v = candidate == null || candidate.isNaN() || candidate.isInfinite() ? defaultValue() : candidate;
        v = Math.max(min, Math.min(max, v));
        v = Math.round((v - min) / step) * step + min;
        v = Math.max(min, Math.min(max, v));
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override public void fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("expected number");
        }
        set(element.getAsDouble());
    }

    @Override public String display() {
        if (isInteger()) return Long.toString(Math.round(value));
        String s = String.format(java.util.Locale.ROOT, "%.2f", value);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }

    @Override public boolean parse(String text) {
        try {
            double d = Double.parseDouble(text.trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) return false;
            if (d < min || d > max) return false;
            set(d);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override public List<String> suggestions() { return List.of(display(), Double.toString(min), Double.toString(max)); }

    @Override public NumberSetting visibleWhen(java.util.function.BooleanSupplier supplier) {
        super.visibleWhen(supplier);
        return this;
    }
}
