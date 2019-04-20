package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;

/** An {@link FixedItemInv} that wraps a vanilla {@link Inventory}. */
public class FixedInventoryViewVanillaWrapper implements FixedItemInvView {
    final Inventory inv;

    public FixedInventoryViewVanillaWrapper(Inventory inv) {
        this.inv = inv;
    }

    public static FixedInventoryViewVanillaWrapper wrapInventory(Inventory inv) {
        return new FixedInventoryViewVanillaWrapper(inv);
    }

    @Override
    public int getSlotCount() {
        return inv.getInvSize();
    }

    @Override
    public ItemStack getInvStack(int slot) {
        return inv.getInvStack(slot);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return inv.isValidInvStack(slot, item);
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        return Math.min(inv.getInvMaxStackAmount(), stack.getMaxAmount());
    }

    @Override
    public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken remToken) {
        // Oddly enough vanilla doesn't support listeners.
        return null;
    }
}
