package alexiil.mc.lib.attributes.item.impl;

import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.GroupedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvAmountChangeListener;
import alexiil.mc.lib.attributes.item.ItemStackCollections;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemStackFilterUtil;

/** A {@link GroupedItemInvView} that wraps a {@link FixedItemInvView}. */
public class GroupedItemInvViewFixedWrapper implements GroupedItemInvView {

    final FixedItemInvView inv;

    public GroupedItemInvViewFixedWrapper(FixedItemInvView inv) {
        this.inv = inv;
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        Set<ItemStack> set = ItemStackCollections.set();
        for (int s = 0; s < inv.getSlotCount(); s++) {
            ItemStack stack = inv.getInvStack(s);
            if (!stack.isEmpty()) {
                set.add(stack);
            }
        }
        return set;
    }

    // TODO: Optimised implementations of getAmount(stack) and getCapacity(stack)

    @Override
    public int getTotalCapacity() {
        int total = 0;
        for (int i = 0; i < inv.getSlotCount(); i++) {
            total += inv.getMaxAmount(i, ItemStack.EMPTY);
        }
        return total;
    }

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        int amount = 0;
        int space = 0;
        int totalSpace = 0;
        boolean totalSpaceValid = true;
        for (int s = 0; s < inv.getSlotCount(); s++) {
            ItemStack stack = inv.getInvStack(s);
            if (!stack.isEmpty()) {
                if (filter.matches(stack)) {
                    amount += stack.getAmount();
                    int max = inv.getMaxAmount(s, stack);
                    space += max - stack.getAmount();
                }
                continue;
            }
            ItemFilter realFilter = AggregateItemFilter.and(filter, inv.getFilterForSlot(s));
            // FIXME: I think this next bit might be a bit broken?
            int max = ItemStackFilterUtil.findMaximumStackAmount(realFilter);
            max = Math.min(max, inv.getMaxAmount(s, stack));
            if (max < 0) {
                // Nothing we can do
                totalSpaceValid = true;
            } else {
                totalSpace += max;
            }
        }
        return new ItemInvStatistic(filter, amount, space, totalSpaceValid ? totalSpace : -1);
    }

    @Override
    public ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        return inv.addListener((i, slot, previous, current) -> {
            if (previous.isEmpty()) {
                if (current.isEmpty()) {
                    // No changes: don't propogate
                } else {
                    int currentAmount = this.getAmount(current);
                    listener.onChange(this, current, currentAmount - current.getAmount(), currentAmount);
                }
            } else {
                if (current.isEmpty()) {
                    int previousAmount = this.getAmount(previous);
                    listener.onChange(this, previous, previousAmount + previous.getAmount(), previousAmount);
                } else {
                    if (ItemStack.areEqual(previous, current)) {
                        int currentAmount = this.getAmount(current);
                        int diff = current.getAmount() - previous.getAmount();
                        listener.onChange(this, current, currentAmount - diff, currentAmount);
                    } else {
                        int currentAmount = this.getAmount(current);
                        int previousAmount = this.getAmount(previous);
                        listener.onChange(this, current, currentAmount - current.getAmount(), currentAmount);
                        listener.onChange(this, previous, previousAmount + previous.getAmount(), previousAmount);
                    }
                }
            }
        }, removalToken);
    }
}
