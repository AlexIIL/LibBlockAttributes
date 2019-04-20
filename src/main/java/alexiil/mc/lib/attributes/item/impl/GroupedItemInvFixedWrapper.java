package alexiil.mc.lib.attributes.item.impl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A {@link GroupedItemInv} wrapper over a {@link FixedItemInv}. This implementation is the naive implementation where
 * every insertion operation will look at every slot in the target inventory in order to insert into the most
 * appropriate slot first. As such the use of this class is discouraged whenever a more efficient version can be used
 * (unless the target inventory has a very small {@link FixedItemInvView#getSlotCount() size}). */
public class GroupedItemInvFixedWrapper extends GroupedItemInvViewFixedWrapper implements GroupedItemInv {

    public GroupedItemInvFixedWrapper(FixedItemInv inv) {
        super(inv);
    }

    final FixedItemInv inv() {
        return (FixedItemInv) inv;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        int invSize = inv.getSlotCount();
        switch (invSize) {
            case 0: {
                // What?
                return ConstantItemFilter.NOTHING;
            }
            case 1: {
                return inv.getFilterForSlot(0);
            }
            case 2: {
                return inv.getFilterForSlot(0).and(inv.getFilterForSlot(1));
            }
            default: {
                List<ItemFilter> filters = new ArrayList<>(invSize);
                for (int i = 0; i < invSize; i++) {
                    filters.add(inv.getFilterForSlot(i));
                }
                return AggregateItemFilter.anyOf(filters);
            }
        }
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        /* Even though there is a giant warning at the top of this class it should still be possible to optimise this
         * implementation a bit more than this very basic version. */

        return simpleDumbBadInsertionToBeRemoved(stack, simulation);
    }

    private ItemStack simpleDumbBadInsertionToBeRemoved(ItemStack stack, Simulation simulation) {
        stack = stack.copy();
        for (int s = 0; s < inv.getSlotCount(); s++) {
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
            if (inv().setInvStack(s, inSlot, simulation)) {
                stack.subtractAmount(addable);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxCount, Simulation simulation) {

        if (maxCount < 0) {
            throw new IllegalArgumentException("maxAmount cannot be negative! (was " + maxCount + ")");
        }
        ItemStack stack = ItemStack.EMPTY;
        if (maxCount == 0) {
            return stack;
        }

        for (int s = 0; s < inv.getSlotCount(); s++) {
            ItemStack invStack = inv.getInvStack(s);
            if (invStack.isEmpty() || !filter.matches(invStack)) {
                continue;
            }
            if (!stack.isEmpty()) {
                if (!ItemStackUtil.areEqualIgnoreAmounts(stack, invStack)) {
                    continue;
                }
            }
            invStack = invStack.copy();

            ItemStack addable = invStack.split(maxCount);
            if (inv().setInvStack(s, invStack, simulation)) {

                if (stack.isEmpty()) {
                    stack = addable;
                } else {
                    stack.addAmount(addable.getAmount());
                }
                maxCount -= addable.getAmount();
                assert maxCount >= 0;
                if (maxCount <= 0) {
                    return stack;
                }
            }
        }

        return stack;
    }
}
