/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.impl.ItemInvModificationTracker;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/** An {@link Inventory} that wraps an {@link FixedItemInv}.
 * <p>
 * One of the {@link Inventory} methods must be overridden by subclasses however:
 * {@link Inventory#canPlayerUseInv(PlayerEntity)}. */
public abstract class InventoryFixedWrapper implements Inventory {

    protected final FixedItemInv inv;
    private final Int2ObjectMap<SlotStatus> slotStatus = new Int2ObjectOpenHashMap<>();

    public InventoryFixedWrapper(FixedItemInv inv) {
        this.inv = inv;
    }

    @Override
    public void clear() {
        for (int s = 0; s < inv.getSlotCount(); s++) {
            inv.setInvStack(s, ItemStack.EMPTY, Simulation.ACTION);
        }
        slotStatus.clear();
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
        SlotStatus prev = slotStatus.remove(slot);
        if (prev != null) {
            prev.process(this, slot);
        }
        ItemStack current = inv.getInvStack(slot);
        SlotStatus status = new SlotStatus(current);
        slotStatus.put(slot, status);
        return status.returned;
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
        SlotStatus status = slotStatus.remove(slot);
        if (status != null) {
            status.validate(this, slot);
        }
        status = new SlotStatus(to.copy(), to);
        slotStatus.put(slot, status);
        setInvStackInternal(slot, status.originalCopy);
    }

    public boolean softSetInvStack(int slot, ItemStack to) {
        SlotStatus status = slotStatus.remove(slot);
        if (status != null) {
            status.validate(this, slot);
        }
        status = new SlotStatus(to.copy(), to);
        if (inv.setInvStack(slot, status.originalCopy, Simulation.ACTION)) {
            slotStatus.put(slot, status);
            return true;
        }
        return false;
    }

    void setInvStackInternal(int slot, ItemStack to) {
        if (!inv.setInvStack(slot, to, Simulation.ACTION)) {
            throw new IllegalStateException("The FixedItemInv " + inv.getClass() + " didn't accept the stack " + to
                + " in slot " + slot + "! The inventory may be in a duped (invalid) state!");
        }
    }

    @Override
    public boolean isValidInvStack(int slot, ItemStack stack) {
        return inv.isItemValidForSlot(slot, stack);
    }

    @Override
    public void markDirty() {
        if (slotStatus.isEmpty()) {
            return;
        }
        for (int slot : slotStatus.keySet().toIntArray()) {
            slotStatus.remove(slot).process(this, slot);
        }
        assert slotStatus.isEmpty();
    }

    static final class SlotStatus {
        /** A copy of the itemstack that was originally seen in the backing {@link FixedItemInv}. */
        final ItemStack originalCopy;

        /** The itemstack that was returned from {@link InventoryFixedWrapper#getInvStack(int)}. */
        final ItemStack returned;

        public SlotStatus(ItemStack current) {
            this(current.copy(), current.copy());
        }

        public SlotStatus(ItemStack originalCopy, ItemStack returned) {
            this.originalCopy = originalCopy;
            this.returned = returned;
        }

        void validate(InventoryFixedWrapper inv, int slot) {
            ItemStack current = inv.inv.getInvStack(slot);
            if (!ItemStack.areEqualIgnoreDamage(originalCopy, current) && !ItemStack.areEqualIgnoreDamage(originalCopy, returned)) {
                throw new IllegalStateException("The inventory has been modifed in two places at once! (\n\tcurrent = "
                    + ItemInvModificationTracker.stackToFullString(current) + ", \n\toriginal = "
                    + ItemInvModificationTracker.stackToFullString(originalCopy) + ", \n\tnew = "
                    + ItemInvModificationTracker.stackToFullString(returned) + ")");
            }
        }

        void process(InventoryFixedWrapper inv, int slot) {
            validate(inv, slot);
            if (ItemStack.areEqualIgnoreDamage(returned, originalCopy)) {
                // Nothing changed
                return;
            }
            inv.setInvStackInternal(slot, returned);
            ItemInvModificationTracker.trackNeverChanging(returned);
        }
    }
}
