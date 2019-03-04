package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IFixedItemInv;
import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;

public final class SimpleFixedItemInvExtractable implements IItemExtractable {

    private final IFixedItemInv inv;

    /** Null means that this can extract from any of the slots. */
    private final int[] slots;

    public SimpleFixedItemInvExtractable(IFixedItemInv inv, int[] slots) {
        this.inv = inv;
        this.slots = slots;
    }

    @Override
    public ItemStack attemptExtraction(IItemFilter filter, int maxCount, Simulation simulation) {

        ItemStack stack = ItemStack.EMPTY;
        if (slots == null) {
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
                if (inv.setInvStack(s, invStack, simulation)) {

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
        } else {
            for (int s : slots) {
                // copy-paste of above
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
                if (inv.setInvStack(s, invStack, simulation)) {
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
        }

        return stack;
    }
}
