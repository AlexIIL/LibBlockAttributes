package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A sub-view of an existing {@link FixedItemInvView}. */
public class SubFixedItemInvView<InvType extends FixedItemInvView> implements FixedItemInvView {

    /** The inventory that is wrapped. */
    protected final InvType inv;

    /** The slots that we use. */
    private final int fromIndex, toIndex;

    public SubFixedItemInvView(InvType inv, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                "fromIndex was greater than toIndex! (" + fromIndex + " > " + toIndex + ")");
        }
        this.inv = inv;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /** @return The slot that the internal {@link #inv} should use. */
    protected int getInternalSlot(int slot) {
        slot += fromIndex;
        if (slot >= toIndex) {
            throw new IllegalArgumentException("The given slot " + (slot - fromIndex)
                + "is greater than the size of this inventory! (" + getSlotCount() + ")");
        }
        return slot;
    }

    @Override
    public int getSlotCount() {
        return toIndex - fromIndex;
    }

    @Override
    public ItemStack getInvStack(int slot) {
        return inv.getInvStack(getInternalSlot(slot));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return inv.isItemValidForSlot(getInternalSlot(slot), stack);
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        return inv.getFilterForSlot(getInternalSlot(slot));
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        return inv.getMaxAmount(getInternalSlot(slot), stack);
    }

    @Override
    public FixedItemInvView getView() {
        if (getClass() == SubFixedItemInvView.class) {
            return this;
        }
        return FixedItemInvView.super.getView();
    }

    @Override
    public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
        FixedItemInvView wrapper = this;
        return inv.addListener((realInv, slot, previous, current) -> {
            assert realInv == inv;
            if (slot >= fromIndex && slot < toIndex) {
                int exposedSlot = slot - fromIndex;
                listener.onChange(wrapper, exposedSlot, previous, current);
            }
        }, removalToken);
    }
}
