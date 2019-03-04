package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.IListenerToken;
import alexiil.mc.lib.attributes.item.IFixedItemInv;
import alexiil.mc.lib.attributes.item.IFixedItemInvView;
import alexiil.mc.lib.attributes.item.IItemInvSlotChangeListener;

/** An {@link IFixedItemInv} that wraps a vanilla {@link Inventory}. */
public class FixedInventoryViewVanillaWrapper implements IFixedItemInvView {
    final Inventory inv;

    FixedInventoryViewVanillaWrapper(Inventory inv) {
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
    public IListenerToken addListener(IItemInvSlotChangeListener listener) {
        // Oddly enough vanilla doesn't support listeners.
        return null;
    }
}
