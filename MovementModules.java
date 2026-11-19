package dev.iris.client.modules;

import dev.iris.client.module.Category;
import dev.iris.client.module.Module;
import dev.iris.client.module.ModuleManager;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ModeSetting;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.util.Format;
import dev.iris.client.util.Mc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Movement quality-of-life modules. Everything here only manipulates the player's own input keys (as if the
 * player pressed them) or watches movement state to warn the player. Nothing alters movement physics.
 */
public final class MovementModules {
    private MovementModules() {}

    public static void register(ModuleManager mm) {
        mm.register(
                new AlwaysSprint(),
                new ToggleSneak(),
                new AutoWalk(),
                new AntiAfk(),
                new DistanceWalked(),
                new FallWarning(),
                new ElytraWarning(),
                new KeyLock(),
                new StuckAlert(),
                new VoidWarning()
        );
    }

    // ------------------------------------------------------------------ shared base

    /** Base for modules that warn the player: a notification plus optional local chat line, rate limited. */
    abstract static class AlertModule extends Module {
        final BoolSetting chat = add(new BoolSetting("Chat message", "Also print the warning as a local chat line.", false));
        final NumberSetting cooldown;
        private long lastAlert;

        AlertModule(String name, String description, double defaultCooldown) {
            super(name, description, Category.MOVEMENT);
            cooldown = add(new NumberSetting("Cooldown", "Minimum seconds between two warnings.", defaultCooldown, 1, 120, 1));
        }

        /** Posts the warning unless the cooldown has not elapsed. Returns true when it was shown. */
        boolean alert(String title, String message) {
            long now = System.currentTimeMillis();
            if (now - lastAlert < (long) (cooldown.dbl() * 1000)) return false;
            lastAlert = now;
            notifications().warn(title, message);
            if (chat.on()) Mc.chatLocal("§e[Iris] §f" + message);
            return true;
        }

        @Override protected void onDisable() { lastAlert = 0; }
    }

    // ------------------------------------------------------------------ Always Sprint

    static final class AlwaysSprint extends Module {
        private final ModeSetting condition = add(new ModeSetting("Condition", "When the sprint key is held down for you.",
                "Moving forward", "Moving forward", "Any movement"));
        private final BoolSetting notSneaking = add(new BoolSetting("Not while sneaking", "Do not sprint while the sneak key is down.", true));
        private final BoolSetting notInWater = add(new BoolSetting("Not in water", "Do not sprint-swim; walk normally in water.", false));
        private boolean held;

        AlwaysSprint() { super("Always Sprint", "Holds the sprint key for you while you move.", Category.MOVEMENT); }

        @Override public String info() { return condition.get(); }

        @Override public void onTick() {
            if (mc.gui.screen() != null || mc.player == null) return;
            boolean moving = mc.options.keyUp.isDown();
            if (!moving && condition.is("Any movement")) {
                moving = mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
            }
            boolean allowed = moving
                    && !(notSneaking.on() && mc.options.keyShift.isDown())
                    && !(notInWater.on() && mc.player.isInWater());
            if (allowed) {
                mc.options.keySprint.setDown(true);
                held = true;
            } else if (held) {
                mc.options.keySprint.setDown(false);
                held = false;
            }
        }

        @Override protected void onDisable() {
            if (held && mc.options != null) mc.options.keySprint.setDown(false);
            held = false;
        }
    }

    // ------------------------------------------------------------------ Toggle Sneak

    static final class ToggleSneak extends Module {
        private final ModeSetting behavior = add(new ModeSetting("Behavior", "When to keep the sneak key down.",
                "Always", "Always", "Only while moving"));
        private final BoolSetting notInWater = add(new BoolSetting("Not in water", "Release sneak while swimming so you do not sink.", true));
        private boolean held;

        ToggleSneak() { super("Toggle Sneak", "Keeps the sneak key down so you do not have to hold it.", Category.MOVEMENT); }

        @Override public String info() { return behavior.get(); }

        @Override public void onTick() {
            if (mc.gui.screen() != null || mc.player == null) return;
            boolean moving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                    || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
            boolean want = (behavior.is("Always") || moving) && !(notInWater.on() && mc.player.isInWater());
            if (want) {
                mc.options.keyShift.setDown(true);
                held = true;
            } else if (held) {
                mc.options.keyShift.setDown(false);
                held = false;
            }
        }

        @Override protected void onDisable() {
            if (held && mc.options != null) mc.options.keyShift.setDown(false);
            held = false;
        }
    }

    // ------------------------------------------------------------------ Auto Walk

    static final class AutoWalk extends Module {
        private final BoolSetting sprint = add(new BoolSetting("Sprint", "Also hold the sprint key while walking.", false));
        private final BoolSetting stopOnDamage = add(new BoolSetting("Stop on damage", "Turn off as soon as you take damage.", true));
        private final NumberSetting stopBelow = add(new NumberSetting("Stop below hearts", "Turn off when health drops to this many hearts (0 = never).", 0, 0, 10, 1));
        private final BoolSetting pauseInScreens = add(new BoolSetting("Pause in menus", "Release the forward key while a menu is open.", true));
        private float lastHealth;
        private boolean heldSprint;

        AutoWalk() { super("Auto Walk", "Holds the forward key for you so you keep walking hands-free.", Category.MOVEMENT); }

        @Override protected void onEnable() {
            lastHealth = mc.player != null ? mc.player.getHealth() : 0f;
        }

        @Override public void onTick() {
            if (mc.player == null) return;
            float health = mc.player.getHealth();
            boolean hurt = stopOnDamage.on() && health < lastHealth - 0.01f;
            boolean low = stopBelow.intValue() > 0 && health <= stopBelow.intValue() * 2f;
            lastHealth = health;
            if (hurt || low) {
                notifications().warn("Auto Walk", hurt ? "Stopped: you took damage." : "Stopped: health is low.");
                setEnabled(false, false);
                return;
            }
            if (mc.gui.screen() != null && pauseInScreens.on()) {
                release();
                return;
            }
            mc.options.keyUp.setDown(true);
            if (sprint.on()) {
                mc.options.keySprint.setDown(true);
                heldSprint = true;
            } else if (heldSprint) {
                mc.options.keySprint.setDown(false);
                heldSprint = false;
            }
        }

        private void release() {
            if (mc.options == null) return;
            mc.options.keyUp.setDown(false);
            if (heldSprint) mc.options.keySprint.setDown(false);
            heldSprint = false;
        }

        @Override protected void onDisable() { release(); }
    }

    // ------------------------------------------------------------------ Anti AFK

    static final class AntiAfk extends Module {
        private final NumberSetting interval = add(new NumberSetting("Interval", "Seconds of inactivity before a nudge.", 45, 5, 300, 5));
        private final ModeSetting action = add(new ModeSetting("Action", "What the small nudge does.",
                "Jump and look", "Jump", "Look", "Jump and look"));
        private final NumberSetting lookAmount = add(new NumberSetting("Look amount", "Degrees to turn on a look nudge.", 8, 2, 30, 1)
                .visibleWhen(() -> !action.is("Jump")));
        private final BoolSetting notifyNudge = add(new BoolSetting("Notify", "Show a notification when a nudge happens.", false));

        private double lastX, lastZ;
        private float lastYaw, lastPitch;
        private int idleTicks;
        private boolean jumpHeld;
        private boolean lookRight = true;
        private boolean primed;

        AntiAfk() { super("Anti AFK", "Makes a tiny jump or head turn after you have been idle for a while.", Category.MOVEMENT); }

        @Override public String info() { return Format.fixed(interval.dbl(), 0) + "s"; }

        @Override protected void onEnable() { primed = false; idleTicks = 0; }

        @Override public void onTick() {
            if (mc.player == null) return;
            if (jumpHeld) {
                mc.options.keyJump.setDown(false);
                jumpHeld = false;
            }
            double x = mc.player.getX(), z = mc.player.getZ();
            float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
            boolean keys = mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown()
                    || mc.options.keyRight.isDown() || mc.options.keyJump.isDown() || mc.options.keyAttack.isDown()
                    || mc.options.keyUse.isDown();
            double dx = x - lastX, dz = z - lastZ;
            boolean active = !primed || keys || mc.gui.screen() != null || dx * dx + dz * dz > 1.0e-4
                    || Math.abs(yaw - lastYaw) > 0.01f || Math.abs(pitch - lastPitch) > 0.01f;
            lastX = x; lastZ = z; lastYaw = yaw; lastPitch = pitch;
            primed = true;
            if (active) {
                idleTicks = 0;
                return;
            }
            if (++idleTicks < interval.intValue() * 20) return;
            idleTicks = 0;
            if (!action.is("Look")) {
                mc.options.keyJump.setDown(true);
                jumpHeld = true;
            }
            if (!action.is("Jump")) {
                float delta = lookAmount.flt() * (lookRight ? 1f : -1f);
                lookRight = !lookRight;
                mc.player.setYRot(yaw + delta);
                lastYaw = mc.player.getYRot();
            }
            if (notifyNudge.on()) notifications().info("Anti AFK", "Nudged to stay active.");
        }

        @Override protected void onDisable() {
            if (jumpHeld && mc.options != null) mc.options.keyJump.setDown(false);
            jumpHeld = false;
            idleTicks = 0;
            primed = false;
        }
    }

    // ------------------------------------------------------------------ Distance Walked

    static final class DistanceWalked extends Module {
        private final NumberSetting milestone = add(new NumberSetting("Milestone", "Blocks between notifications.", 500, 50, 5000, 50));
        private final BoolSetting vertical = add(new BoolSetting("Include vertical", "Count up/down movement as well as horizontal.", false));
        private final BoolSetting resetOnLeave = add(new BoolSetting("Reset on world leave", "Start from zero in every world or server.", true));
        private final BoolSetting chat = add(new BoolSetting("Chat message", "Also print milestones as a local chat line.", false));

        private double lastX, lastY, lastZ;
        private boolean hasLast;
        private double total;
        private long nextMilestone;

        DistanceWalked() { super("Distance Walked", "Counts the distance you travel and notifies you at every milestone.", Category.MOVEMENT); }

        @Override public String info() { return (long) total + " blocks"; }

        @Override protected void onEnable() { hasLast = false; }

        @Override public void onTick() {
            if (mc.player == null) return;
            double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
            if (hasLast) {
                double dx = x - lastX, dz = z - lastZ, dy = vertical.on() ? y - lastY : 0;
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                // Larger single-tick jumps are teleports, dimension changes or respawns, not walking.
                if (d < 8.0) total += d;
            }
            lastX = x; lastY = y; lastZ = z;
            hasLast = true;

            long step = Math.max(1, milestone.intValue());
            if (nextMilestone <= 0) nextMilestone = (long) (total / step + 1) * step;
            if (total >= nextMilestone) {
                String msg = "You have travelled " + nextMilestone + " blocks.";
                notifications().success("Distance Walked", msg);
                if (chat.on()) Mc.chatLocal("§a[Iris] §f" + msg);
                nextMilestone = (long) (total / step + 1) * step;
            }
        }

        @Override public void onWorldLeave() {
            hasLast = false;
            if (resetOnLeave.on()) {
                total = 0;
                nextMilestone = 0;
            }
        }

        @Override protected void onDisable() {
            hasLast = false;
            total = 0;
            nextMilestone = 0;
        }
    }

    // ------------------------------------------------------------------ Fall Warning

    static final class FallWarning extends AlertModule {
        private final ModeSetting trigger = add(new ModeSetting("Trigger", "Warn after a fixed distance, or only when the fall could kill you.",
                "Distance", "Distance", "Lethal"));
        private final NumberSetting minFall = add(new NumberSetting("Min distance", "Blocks fallen before warning.", 12, 4, 60, 1)
                .visibleWhen(() -> trigger.is("Distance")));
        private final BoolSetting checkBelow = add(new BoolSetting("Check landing", "Scan below you and stay quiet if you will land in water.", true));

        private double peakY;
        private boolean falling;
        private boolean handled;

        FallWarning() {
            super("Fall Warning", "Warns during a long fall unless there is water to land in.", 5);
        }

        @Override public String info() { return trigger.is("Lethal") ? "Lethal" : Format.fixed(minFall.dbl(), 0) + " blocks"; }

        @Override public void onTick() {
            if (mc.player == null || mc.level == null) return;
            if (mc.player.onGround() || mc.player.isInWater() || mc.player.isFallFlying()) {
                falling = false;
                handled = false;
                return;
            }
            double y = mc.player.getY();
            if (!falling) {
                falling = true;
                peakY = y;
            }
            if (y > peakY) peakY = y;
            if (handled) return;

            double fallen = peakY - y;
            boolean hit = trigger.is("Lethal")
                    ? fallen - 3.0 >= mc.player.getHealth()
                    : fallen >= minFall.dbl();
            if (!hit || mc.player.getDeltaMovement().y > -0.3) return;
            handled = true;

            String below = "ground";
            if (checkBelow.on()) {
                below = landingKind();
                if (below.equals("water")) return;
            }
            alert("Fall Warning", "You fell " + (int) fallen + " blocks" + (below.equals("lava") ? " and lava is below you!" : "! No water below."));
        }

        /** Scans straight down for the first non-air block: "water", "lava", "ground" or "void" if none is found. */
        private String landingKind() {
            BlockPos pos = mc.player.blockPosition();
            for (int i = 1; i <= 128; i++) {
                BlockState s = mc.level.getBlockState(pos.below(i));
                if (s.getBlock() == Blocks.WATER) return "water";
                if (s.getBlock() == Blocks.LAVA) return "lava";
                if (!s.isAir()) return "ground";
            }
            return "void";
        }

        @Override protected void onDisable() {
            super.onDisable();
            falling = false;
            handled = false;
        }
    }

    // ------------------------------------------------------------------ Elytra Warning

    static final class ElytraWarning extends AlertModule {
        private final NumberSetting percent = add(new NumberSetting("Warn at", "Durability percentage at which to warn.", 15, 5, 50, 1));
        private final BoolSetting onlyGliding = add(new BoolSetting("Only while gliding", "Stay quiet while you are not flying with the elytra.", true));
        private final NumberSetting critical = add(new NumberSetting("Critical uses", "Always warn (loudly) when this few uses remain.", 10, 1, 50, 1));

        ElytraWarning() {
            super("Elytra Warning", "Warns when your equipped elytra is close to breaking.", 20);
        }

        @Override public void onTick() {
            if (mc.player == null) return;
            ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.isEmpty() || !chest.is(Items.ELYTRA)) return;
            int max = chest.getMaxDamage();
            if (max <= 0) return;
            int left = max - chest.getDamageValue();
            boolean isCritical = left <= critical.intValue();
            if (onlyGliding.on() && !isCritical && !mc.player.isFallFlying()) return;
            double pct = 100.0 * left / max;
            if (pct > percent.dbl() && !isCritical) return;
            if (onlyGliding.on() && !mc.player.isFallFlying() && !isCritical) return;
            alert(isCritical ? "Elytra CRITICAL" : "Elytra Warning",
                    "Elytra durability at " + (int) pct + "% (" + left + " uses left).");
        }
    }

    // ------------------------------------------------------------------ Key Lock

    static final class KeyLock extends Module {
        private final BoolSetting jump = add(new BoolSetting("Lock jump", "Ignore the jump key.", true));
        private final BoolSetting sneak = add(new BoolSetting("Lock sneak", "Ignore the sneak key.", false));
        private final BoolSetting sprint = add(new BoolSetting("Lock sprint", "Ignore the sprint key.", false));
        private final BoolSetting backward = add(new BoolSetting("Lock backward", "Ignore the backward key.", false));
        private final BoolSetting strafe = add(new BoolSetting("Lock strafe", "Ignore the left and right keys.", false));

        KeyLock() { super("Key Lock", "Ignores chosen movement keys, e.g. to avoid accidental jumps while building.", Category.MOVEMENT); }

        @Override public String info() {
            int n = (jump.on() ? 1 : 0) + (sneak.on() ? 1 : 0) + (sprint.on() ? 1 : 0) + (backward.on() ? 1 : 0) + (strafe.on() ? 1 : 0);
            return n + " locked";
        }

        @Override public void onTick() {
            if (mc.options == null) return;
            if (jump.on()) mc.options.keyJump.setDown(false);
            if (sneak.on()) mc.options.keyShift.setDown(false);
            if (sprint.on()) mc.options.keySprint.setDown(false);
            if (backward.on()) mc.options.keyDown.setDown(false);
            if (strafe.on()) {
                mc.options.keyLeft.setDown(false);
                mc.options.keyRight.setDown(false);
            }
        }
    }

    // ------------------------------------------------------------------ Stuck Alert

    static final class StuckAlert extends AlertModule {
        private final NumberSetting seconds = add(new NumberSetting("Time", "Seconds of pushing without moving before warning.", 4, 2, 15, 1));
        private double lastX, lastZ;
        private boolean primed;
        private int stuckTicks;

        StuckAlert() {
            super("Stuck Alert", "Warns when you hold a movement key but are not actually moving.", 15);
        }

        @Override public void onTick() {
            if (mc.player == null) return;
            double x = mc.player.getX(), z = mc.player.getZ();
            double dx = x - lastX, dz = z - lastZ;
            lastX = x; lastZ = z;
            boolean pushing = mc.gui.screen() == null && !mc.player.isPassenger()
                    && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                    || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown());
            if (!primed || !pushing || dx * dx + dz * dz > 1.0e-4) {
                primed = true;
                stuckTicks = 0;
                return;
            }
            if (++stuckTicks == seconds.intValue() * 20) {
                alert("Stuck Alert", "You are pushing but not moving: blocked, in a web, or lagging?");
            }
        }

        @Override public void onWorldLeave() { primed = false; stuckTicks = 0; }

        @Override protected void onDisable() {
            super.onDisable();
            primed = false;
            stuckTicks = 0;
        }
    }

    // ------------------------------------------------------------------ Void Warning

    static final class VoidWarning extends AlertModule {
        private final ModeSetting level = add(new ModeSetting("Level", "Auto picks the right height for the current dimension.", "Auto", "Auto", "Custom"));
        private final NumberSetting customY = add(new NumberSetting("Custom Y", "Warn when falling below this Y level.", -64, -64, 64, 1)
                .visibleWhen(() -> level.is("Custom")));
        private final BoolSetting flash = add(new BoolSetting("Screen flash", "Flash the screen edges while the warning is active.", true));
        private final NumberSetting flashColorHolder = null == null ? null : null;

        VoidWarning() {
            super("Void Warning", "Warns when you are falling below the bottom of the world.", 4);
        }

        @Override public void onTick() {
            if (mc.player == null || mc.level == null) return;
            if (mc.player.onGround() || mc.player.isFallFlying()) return;
            if (mc.player.getDeltaMovement().y > -0.3) return;
            double threshold = level.is("Custom") ? customY.dbl() : autoThreshold();
            if (mc.player.getY() < threshold) {
                alert("Void Warning", "You are falling into the void!");
            }
        }

        /** The overworld's void starts at -64; the Nether and End have their floor at 0. */
        private double autoThreshold() {
            String dim = String.valueOf(mc.level.dimension());
            return dim.contains("overworld") ? -64 : 0;
        }
    }
}
