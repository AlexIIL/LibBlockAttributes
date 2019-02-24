package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IFixedItemInv;
import alexiil.mc.lib.attributes.item.IFixedItemInvView;
import alexiil.mc.lib.attributes.item.IItemInsertable;
import alexiil.mc.lib.attributes.item.ItemStackUtil;

import it.unimi.dsi.fastutil.ints.IntList;

/** An {@link IItemInsertable} wrapper over an {@link IFixedItemInv}. This implementation is the naive implementation
 * where every insertion operation will look at every slot in the target inventory in order to insert into the most
 * appropriate slot first. As such the use of this class is discouraged whenever a more efficient version can be made
 * (unless the target inventory has a very small {@link IFixedItemInvView#getInvSize() size}. */
public final class SimpleFixedInvInsertable implements IItemInsertable {

    private final IFixedItemInv inv;

    /** Null means that this can insert into any of the slots. */
    private final int[] slots;

    public SimpleFixedInvInsertable(IFixedItemInv inv, int[] slots) {
        this.inv = inv;
        this.slots = slots;
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        // ItemStack leftover = stack.copy();
        //
        // // First: scan the available slots to see if we can add to an existing stack
        //
        // IntList slotsModified = new IntArrayList();
        //
        // if (slots == null) {
        // for (int s = 0; s < inv.getInvSize(); s++) {
        // attemptAddToExisting(slotsModified, s, leftover, simulation);
        // }
        // } else {
        // for (int s : slots) {
        // attemptAddToExisting(slotsModified, s, leftover, simulation);
        // }
        // }

        return simpleDumbBadInsertionToBeRemoved(stack, simulation);
    }

    private void attemptAddToExisting(IntList slotsModified, int s, ItemStack leftover, Simulation simulation) {
        ItemStack current = inv.getInvStack(s);

    }

    private ItemStack simpleDumbBadInsertionToBeRemoved(ItemStack stack, Simulation simulation) {
        stack = stack.copy();
        if (slots == null) {
            for (int s = 0; s < inv.getInvSize(); s++) {
                ItemStack inSlot = inv.getInvStack(s);
                int current = inSlot.isEmpty() ? 0 : inSlot.getAmount();
                int max = Math.min(current + stack.getAmount(), inv.getMaxAmount(s, stack));
                int addable = max - current;
                if (addable <= 0) {
                    continue;
                }
                if (current > 0 && !ItemStackUtil.areEqualIgnoreAmounts(stack, inSlot)) {
                    continue;
                }
                if (inSlot.isEmpty()) {
                    inSlot = stack.copy();
                    inSlot.setAmount(addable);
                } else {
                    inSlot.addAmount(addable);
                }
                if (inv.setInvStack(s, inSlot, simulation)) {
                    stack.subtractAmount(addable);
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        } else {
            for (int s : slots) {
                // Copy of above
                ItemStack inSlot = inv.getInvStack(s);
                int current = inSlot.isEmpty() ? 0 : inSlot.getAmount();
                int max = Math.min(current + stack.getAmount(), inv.getMaxAmount(s, inSlot));
                int addable = max - current;
                if (addable <= 0) {
                    continue;
                }
                if (current > 0 && !ItemStackUtil.areEqualIgnoreAmounts(stack, inSlot)) {
                    continue;
                }
                if (inSlot.isEmpty()) {
                    inSlot = stack.copy();
                    inSlot.setAmount(addable);
                } else {
                    inSlot.addAmount(addable);
                }
                if (inv.setInvStack(s, inSlot, simulation)) {
                    stack = stack.split(addable);
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return stack;
    }
}
