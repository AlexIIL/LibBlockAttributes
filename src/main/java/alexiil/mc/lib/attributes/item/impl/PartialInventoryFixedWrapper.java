package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;

/** An {@link Inventory} that wraps an {@link FixedItemInv}.
 * <p>
 * Two of the {@link Inventory} methods must be overridden by subclasses however: {@link Inventory#markDirty()} and
 * {@link Inventory#canPlayerUseInv(net.minecraft.entity.player.PlayerEntity)}. */
public abstract class PartialInventoryFixedWrapper implements Inventory {

    private final FixedItemInv inv;

    public PartialInventoryFixedWrapper(FixedItemInv inv) {
        this.inv = inv;
    }

    @Override
    public void clear() {
        for (int s = 0; s < inv.getSlotCount(); s++) {
            inv.setInvStack(s, ItemStack.EMPTY, Simulation.ACTION);
        }
    }

    @Override
    public int getInvSize() {
        return inv.getSlotCount();
    }

    @Override
    public boolean isInvEmpty() {
        for (int s = 0; s < inv.getSlotCount(); s++) {
            if (!inv.getInvStack(s).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getInvStack(int slot) {
        return inv.getInvStack(slot);
    }

    @Override
    public ItemStack takeInvStack(int slot, int amount) {
        ItemStack stack = getInvStack(slot);
        ItemStack split = stack.split(amount);
        setInvStack(slot, stack);
        return split;
    }

    @Override
    public ItemStack removeInvStack(int slot) {
        ItemStack stack = getInvStack(slot);
        setInvStack(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setInvStack(int slot, ItemStack to) {
        inv.setInvStack(slot, to, Simulation.ACTION);
    }

    @Override
    public boolean isValidInvStack(int slot, ItemStack stack) {
        return inv.isItemValidForSlot(slot, stack);
    }
}
