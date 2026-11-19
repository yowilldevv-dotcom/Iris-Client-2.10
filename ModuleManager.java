package dev.iris.client.module;

import dev.iris.client.core.Iris;
import dev.iris.client.render.Draw;
import dev.iris.client.util.Input;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Registry and dispatcher for all modules. Hooks are isolated: one failing module never affects the others. */
public final class ModuleManager {
    private static final int MAX_CONSECUTIVE_ERRORS = 5;

    private final List<Module> all = new ArrayList<>();
    private final Map<String, Module> byName = new HashMap<>();
    private Module[] enabledCache = new Module[0];
    private boolean worldWasLoaded;

    public void register(Module... modules) {
        for (Module m : modules) {
            String id = m.id();
            if (byName.putIfAbsent(id, m) != null) {
                throw new IllegalStateException("Duplicate module name: " + m.name());
            }
            all.add(m);
        }
        all.sort(Comparator.comparing(Module::name, String.CASE_INSENSITIVE_ORDER));
    }

    public List<Module> all() { return List.copyOf(all); }
    public int count() { return all.size(); }
    public Module[] enabled() { return enabledCache; }

    public Module get(String name) {
        return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT).replace(' ', '-'));
    }

    public List<Module> byCategory(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : all) if (m.category() == category) out.add(m);
        return out;
    }

    /** Search by name or description. Name matches rank before description matches. */
    public List<Module> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return all();
        List<Module> starts = new ArrayList<>();
        List<Module> contains = new ArrayList<>();
        List<Module> desc = new ArrayList<>();
        for (Module m : all) {
            String n = m.name().toLowerCase(Locale.ROOT);
            if (n.startsWith(q)) starts.add(m);
            else if (n.contains(q)) contains.add(m);
            else if (m.description().toLowerCase(Locale.ROOT).contains(q)) desc.add(m);
        }
        starts.addAll(contains);
        starts.addAll(desc);
        return starts;
    }

    void stateChanged(Module module) {
        List<Module> list = new ArrayList<>();
        for (Module m : all) if (m.isEnabled()) list.add(m);
        enabledCache = list.toArray(new Module[0]);
    }

    /** Client tick: polls keybinds, ticks enabled modules and detects world changes. */
    public void tick(Minecraft mc) {
        boolean worldLoaded = mc.player != null && mc.level != null;
        if (worldWasLoaded && !worldLoaded) {
            for (Module m : enabledCache) guard(m, "world leave", m::onWorldLeave);
        }
        worldWasLoaded = worldLoaded;

        pollKeys(mc);

        for (Module m : enabledCache) {
            if (!m.ticksInCurrentState()) continue;
            if (guard(m, "tick", m::onTick)) m.consecutiveErrors = 0;
        }
    }

    public void render(Draw draw, float partialTick) {
        for (Module m : enabledCache) {
            if (!m.ticksInCurrentState()) continue;
            guard(m, "render", () -> m.onRender(draw, partialTick));
        }
    }

    public boolean allowIncomingChat(String plain) {
        for (Module m : enabledCache) {
            try {
                if (!m.allowIncomingChat(plain)) return false;
            } catch (Throwable t) {
                Iris.LOGGER.warn("Module {} chat hook failed", m.name(), t);
            }
        }
        return true;
    }

    /** Returns the message to send, or null if a module cancelled it. */
    public String outgoingChat(String message) {
        String current = message;
        for (Module m : enabledCache) {
            try {
                current = m.onOutgoingChat(current);
                if (current == null) return null;
            } catch (Throwable t) {
                Iris.LOGGER.warn("Module {} outgoing chat hook failed", m.name(), t);
            }
        }
        return current;
    }

    private void pollKeys(Minecraft mc) {
        boolean canPoll = mc.gui.screen() == null && mc.isWindowActive();
        for (Module m : all) {
            if (m.key() < 0) { m.keyWasDown = false; continue; }
            boolean down = canPoll && Input.isKeyDown(m.key());
            if (down && !m.keyWasDown) m.toggle();
            m.keyWasDown = down;
        }
    }

    /** Runs a hook; on repeated failure the module is disabled and the user is told. Returns true on success. */
    private boolean guard(Module m, String phase, Runnable hook) {
        try {
            hook.run();
            return true;
        } catch (Throwable t) {
            m.consecutiveErrors++;
            Iris.LOGGER.error("Module {} threw during {} ({} in a row)", m.name(), phase, m.consecutiveErrors, t);
            if (m.consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                m.setEnabled(false, false);
                Iris.notifications().error("Module disabled", m.name() + " was turned off after repeated errors.");
            }
            return false;
        }
    }

    /** Disables everything (without changing saved state) so each module can restore game options. */
    public void shutdown() {
        for (Module m : enabledCache) m.safeDisableHook();
    }

    /** Disables all enabled modules (panic). Returns how many were turned off. */
    public int disableAll() {
        int n = 0;
        for (Module m : enabledCache.clone()) {
            m.setEnabled(false, false);
            n++;
        }
        return n;
    }
}
