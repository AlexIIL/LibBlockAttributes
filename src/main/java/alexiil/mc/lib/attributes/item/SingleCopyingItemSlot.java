package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.FixedItemInv.CopyingFixedItemInv;

public class SingleCopyingItemSlot extends SingleItemSlot {

    SingleCopyingItemSlot(CopyingFixedItemInv backingView, int slot) {
        super(backingView, slot);
    }

    @Override
    public CopyingFixedItemInv getBackingInv() {
        return (CopyingFixedItemInv) super.getBackingInv();
    }

    /** Adds the given listener to the backing inventory, such that the
     * {@link ItemInvSlotChangeListener#onChange(FixedItemInvView, int, ItemStack, ItemStack)} will be called every time
     * that this inventory changes. However if this inventory doesn't support listeners then this will return a null
     * {@link ListenerToken token}.
     * 
     * @param removalToken A token that will be called whenever the given listener is removed from this inventory (or if
     *            this inventory itself is unloaded or otherwise invalidated).
     * @return A token that represents the listener, or null if the listener could not be added. */
    public final ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
        return getBackingInv().addListener((realInv, s, previous, current) -> {
            assert realInv == backingView;
            if (slot == s) {
                listener.onChange(realInv, slot, previous, current);
            }
        }, removalToken);
    }
}
