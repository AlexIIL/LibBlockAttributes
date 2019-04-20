package alexiil.mc.lib.attributes.item.impl;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;

/** Default implementation for {@link FixedItemInvView#getSubInv(int, int)}. */
public class SubFixedItemInvView extends AbstractPartialFixedItemInvView {

    /** The slots that we use. */
    private final int fromIndex, toIndex;

    public SubFixedItemInvView(FixedItemInvView inv, int fromIndex, int toIndex) {
        super(inv);
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                "fromIndex was greater than toIndex! (" + fromIndex + " > " + toIndex + ")");
        }
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /** @return The slot that the internal {@link #inv} should use. */
    @Override
    protected final int getInternalSlot(int slot) {
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
    public FixedItemInvView getView() {
        if (getClass() == SubFixedItemInvView.class) {
            return this;
        }
        return super.getView();
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
