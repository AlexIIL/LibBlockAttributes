package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface ItemInvAmountChangeListener {

    /** @param inv The inventory that changed
     * @param stack The {@link ItemStack} whose amount changed.
     * @param previous The previous amount of the given stack.
     * @param current The new amount of the given stack. */
    void onChange(GroupedItemInvView inv, ItemStack stack, int previous, int current);
}
