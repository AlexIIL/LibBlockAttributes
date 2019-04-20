package alexiil.mc.lib.attributes.item.impl;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.GroupedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvAmountChangeListener;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** {@link GroupedItemInvView} for an empty inventory. */
public enum EmptyGroupedItemInv implements GroupedItemInv {
    INSTANCE;

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        return new ItemInvStatistic(filter, 0, 0, 0);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        return Collections.emptySet();
    }

    @Override
    public int getAmount(ItemFilter filter) {
        return 0;
    }

    @Override
    public int getAmount(ItemStack stack) {
        return 0;
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return 0;
    }

    @Override
    public int getSpace(ItemStack stack) {
        return 0;
    }

    @Override
    public int getTotalCapacity() {
        return 0;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return ConstantItemFilter.NOTHING;
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return stack;
    }

    @Override
    public ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return ItemStack.EMPTY;
    }

    @Override
    public ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        // We don't need to keep track of the listener because this empty inventory never changes.
        return () -> {
            // (And we don't need to do anything when the listener is removed)
        };
        // Never call the removal token as it's unnecessary (and saves the caller from re-adding it every tick)
    }
}
