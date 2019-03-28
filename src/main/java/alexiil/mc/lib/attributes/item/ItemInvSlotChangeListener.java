package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

public interface ItemInvSlotChangeListener {

    /** @param inv The inventory that changed
     * @param slot The slot that changed
     * @param previous The previous {@link ItemStack}.
     * @param current The new {@link ItemStack} */
    void onChange(FixedItemInvView inv, int slot, ItemStack previous, ItemStack current);
}
