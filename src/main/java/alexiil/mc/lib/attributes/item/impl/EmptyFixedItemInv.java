package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** An {@link FixedItemInv} with no slots. Because this inventory is unmodifiable this also doubles as the empty
 * implementation for {@link FixedItemInvView}. */
public enum EmptyFixedItemInv implements FixedItemInv {
    INSTANCE;

    private static IllegalArgumentException throwInvalidSlotException() {
        throw new IllegalArgumentException("There are no valid slots in this empty inventory!");
    }

    @Override
    public int getSlotCount() {
        return 0;
    }

    @Override
    public ItemStack getInvStack(int slot) {
        throw throwInvalidSlotException();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        throw throwInvalidSlotException();
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        throw throwInvalidSlotException();
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        throw throwInvalidSlotException();
    }

    @Override
    public GroupedItemInv getGroupedInv() {
        return EmptyGroupedItemInv.INSTANCE;
    }

    @Override
    public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken remToken) {
        // We don't need to keep track of the listener because this empty inventory never changes.
        return () -> {
            // (And we don't need to do anything when the listener is removed)
        };
        // Never call the removal token as it's unnecessary (and saves the caller from re-adding it every tick)
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        throw throwInvalidSlotException();
    }

    @Override
    public FixedItemInvView getView() {
        return this;
    }

    @Override
    public ItemInsertable getInsertable() {
        return RejectingItemInsertable.NULL;
    }

    @Override
    public ItemExtractable getExtractable() {
        return EmptyItemExtractable.NULL;
    }
}
