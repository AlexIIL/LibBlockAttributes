package alexiil.mc.lib.attributes.item;

import java.util.Objects;

import net.minecraft.item.ItemStack;

public enum ItemStackUtil {
    ;

    public static boolean areEqualIgnoreAmounts(ItemStack a, ItemStack b) {
        if (a.isEmpty()) {
            return b.isEmpty();
        }
        if (b.isEmpty()) {
            return false;
        }
        return a.getItem() == b.getItem() && Objects.equals(a.getTag(), b.getTag());
    }
}
