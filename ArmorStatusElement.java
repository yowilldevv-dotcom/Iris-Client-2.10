package dev.iris.client.hud.elements;

import dev.iris.client.hud.HudElement;
import dev.iris.client.render.Colors;
import dev.iris.client.render.Draw;
import dev.iris.client.setting.BoolSetting;
import dev.iris.client.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Armor and held item icons with durability. */
public final class ArmorStatusElement extends HudElement {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    private final ModeSetting layout = add(new ModeSetting("Layout", "Vertical or horizontal.", "Vertical", "Vertical", "Horizontal"));
    private final ModeSetting durability = add(new ModeSetting("Durability", "How durability is shown.", "Percent", "Percent", "Number", "None"));
    private final BoolSetting includeHand = add(new BoolSetting("Held item", "Include the item in your main hand.", true));
    private final List<ItemStack> stacks = new ArrayList<>();

    public ArmorStatusElement() {
        super("Armor Status", "Shows armor pieces and their durability.", 0.01, 0.45, true);
    }

    @Override public void prepare(boolean preview) {
        stacks.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (EquipmentSlot s : SLOTS) {
            if (s == EquipmentSlot.MAINHAND && !includeHand.on()) continue;
            ItemStack st = mc.player.getItemBySlot(s);
            if (!st.isEmpty()) stacks.add(st);
        }
    }

    private boolean vertical() { return layout.is("Vertical"); }

    @Override public int width(Draw d) {
        if (stacks.isEmpty()) return 20;
        return vertical() ? 18 + (durability.is("None") ? 0 : 34) : stacks.size() * 36;
    }

    @Override public int height(Draw d) {
        if (stacks.isEmpty()) return 16;
        return vertical() ? stacks.size() * 18 : 30;
    }

    @Override public void render(Draw d, boolean preview) {
        if (stacks.isEmpty()) {
            if (preview) d.text("Armor", 0, 4, color.argb(), shadow.on());
            return;
        }
        int i = 0;
        for (ItemStack st : stacks) {
            int x = vertical() ? 0 : i * 36;
            int y = vertical() ? i * 18 : 0;
            d.item(st, x, y);
            if (st.isDamageableItem() && !durability.is("None")) {
                int max = st.getMaxDamage();
                int left = max - st.getDamageValue();
                String s = durability.is("Percent") ? Math.round(left * 100f / max) + "%" : Integer.toString(left);
                int c = Colors.health(left / (float) max);
                if (vertical()) d.text(s, x + 20, y + 4, c, shadow.on());
                else d.textCentered(s, x + 8, y + 18, c, shadow.on());
            }
            i++;
        }
    }
}
