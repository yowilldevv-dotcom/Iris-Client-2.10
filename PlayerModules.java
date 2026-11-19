package dev.iris.client.modules;

import dev.iris.client.module.Category;
import dev.iris.client.module.Module;
import dev.iris.client.module.ModuleManager;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.NumberSetting;
import dev.iris.client.util.Mc;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Player state alerts. Each alert has a cooldown so it never spams. */
public final class PlayerModules {
    /** Last recorded death position (x, y, z), or null if none this session. */
    public static volatile int[] lastDeath;

    private PlayerModules() {}

    public static void register(ModuleManager mm) {
        mm.register(new LowHealthAlert(), new LowHungerAlert(), new DurabilityAlert(), new TotemReminder(),
                new InventoryFullAlert(), new DeathPosition());
    }

    /** Base for alerts that fire when a condition becomes true and re-arm after it clears. */
    private abstract static class Alert extends Module {
        private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Seconds before the same alert can repeat.", 30, 5, 300, 5));
        private long lastFired;
        private boolean wasActive;
        private int tickCounter;

        Alert(String name, String description) { super(name, description, Category.PLAYER); }

        abstract boolean condition();

        abstract String title();

        abstract String message();

        @Override protected void onDisable() { wasActive = false; }

        @Override public void onWorldLeave() { wasActive = false; }

        @Override public void onTick() {
            if (++tickCounter % 10 != 0) return;
            boolean active = condition();
            long now = System.currentTimeMillis();
            if (active && (!wasActive || now - lastFired >= cooldown.dbl() * 1000)) {
                if (!wasActive || now - lastFired >= cooldown.dbl() * 1000) {
                    notifications().warn(title(), message());
                    lastFired = now;
                }
            }
            wasActive = active;
        }
    }

    static final class LowHealthAlert extends Alert {
        private final NumberSetting threshold = add(new NumberSetting("Threshold", "Warn at or below this many hearts.", 4, 1, 10, 0.5));

        LowHealthAlert() { super("Low Health Alert", "Warns when your health is low."); }

        @Override boolean condition() { return mc.player.getHealth() <= threshold.dbl() * 2 && mc.player.getHealth() > 0; }
        @Override String title() { return "Low health"; }
        @Override String message() { return Math.round(mc.player.getHealth() / 2f * 10) / 10f + " hearts left"; }
    }

    static final class LowHungerAlert extends Alert {
        private final NumberSetting threshold = add(new NumberSetting("Threshold", "Warn at or below this food level.", 6, 1, 19, 1));

        LowHungerAlert() { super("Low Hunger Alert", "Warns when your food level is low."); }

        @Override boolean condition() { return mc.player.getFoodData().getFoodLevel() <= threshold.intValue(); }
        @Override String title() { return "Hungry"; }
        @Override String message() { return "Food level " + mc.player.getFoodData().getFoodLevel() + "/20"; }
    }

    static final class DurabilityAlert extends Alert {
        private final NumberSetting percent = add(new NumberSetting("Percent", "Warn when a worn or held item has this much durability left or less.", 10, 1, 50, 1));
        private final BoolSetting armor = add(new BoolSetting("Armor", "Check armor pieces.", true));
        private final BoolSetting hand = add(new BoolSetting("Held item", "Check the main-hand item.", true));
        private String culprit = "";

        DurabilityAlert() { super("Low Durability Alert", "Warns when armor or your held tool is about to break."); }

        private boolean low(ItemStack st) {
            if (st.isEmpty() || !st.isDamageableItem()) return false;
            int max = st.getMaxDamage();
            return max > 0 && (max - st.getDamageValue()) * 100.0 / max <= percent.dbl();
        }

        @Override boolean condition() {
            culprit = "";
            if (hand.on() && low(mc.player.getMainHandItem())) { culprit = mc.player.getMainHandItem().getHoverName().getString(); return true; }
            if (armor.on()) {
                for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack st = mc.player.getItemBySlot(s);
                    if (low(st)) { culprit = st.getHoverName().getString(); return true; }
                }
            }
            return false;
        }

        @Override String title() { return "Low durability"; }
        @Override String message() { return culprit + " is almost broken"; }
    }

    static final class TotemReminder extends Alert {
        private final NumberSetting health = add(new NumberSetting("Health", "Only remind when at or below this many hearts (10 = always).", 10, 2, 10, 1));

        TotemReminder() { super("Totem Reminder", "Reminds you when your off hand has no Totem of Undying."); }

        @Override boolean condition() {
            return mc.player.getHealth() <= health.dbl() * 2 && !mc.player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.TOTEM_OF_UNDYING);
        }
        @Override String title() { return "No totem"; }
        @Override String message() { return "Your off hand has no Totem of Undying"; }
    }

    static final class InventoryFullAlert extends Alert {
        InventoryFullAlert() { super("Inventory Full Alert", "Warns when every main inventory slot is occupied."); }

        @Override boolean condition() {
            var inv = mc.player.getInventory();
            for (int i = 0; i < 36; i++) if (inv.getItem(i).isEmpty()) return false;
            return true;
        }
        @Override String title() { return "Inventory full"; }
        @Override String message() { return "No free slots left"; }
    }

    static final class DeathPosition extends Module {
        private final BoolSetting chat = add(new BoolSetting("Chat message", "Also print the position in chat.", true));
        private boolean wasAlive = true;
        private double lx, ly, lz;

        DeathPosition() { super("Death Position", "Remembers where you died and tells you after respawning.", Category.PLAYER); }

        @Override protected void onEnable() { wasAlive = true; }

        @Override public void onTick() {
            boolean alive = mc.player.getHealth() > 0 && !mc.player.isDeadOrDying();
            if (alive) {
                if (!wasAlive) {
                    String pos = (int) lx + " " + (int) ly + " " + (int) lz;
                    lastDeath = new int[]{(int) lx, (int) ly, (int) lz};
                    notifications().info("You died at", pos);
                    if (chat.on()) Mc.chatLocal("§8[§bIris§8]§r §7You died at §f" + pos);
                }
                lx = mc.player.getX();
                ly = mc.player.getY();
                lz = mc.player.getZ();
            }
            wasAlive = alive;
        }
    }
}
