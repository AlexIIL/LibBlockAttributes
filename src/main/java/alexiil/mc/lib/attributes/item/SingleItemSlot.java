package alexiil.mc.lib.attributes.item;

import java.util.function.Function;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A delegating accessor of a single slot in a {@link FixedItemInv}. */
public final class SingleItemSlot extends SingleItemSlotView implements ItemTransferable {

    SingleItemSlot(FixedItemInv backingView, int slot) {
        super(backingView, slot);
    }

    @Override
    public final FixedItemInv getBackingInv() {
        return (FixedItemInv) this.backingView;
    }

    /** Sets the stack in this slot to the given stack.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         FixedItemInvView.isItemValidForSlot(int, ItemStack) test). */
    public final boolean set(ItemStack to, Simulation simulation) {
        return getBackingInv().setInvStack(slot, to, simulation);
    }

    /** Sets the stack in the given slot to the given stack, or throws an exception if it was not permitted. */
    public final void forceSet(ItemStack to) {
        getBackingInv().forceSetInvStack(slot, to);
    }

    /** Applies the given function to the stack held in the slot, and uses {@link #forceSet(ItemStack)} on the result
     * (Which will throw an exception if the returned stack is not valid for this inventory). */
    public final void modify(Function<ItemStack, ItemStack> function) {
        getBackingInv().modifySlot(slot, function);
    }

    @Override
    public final ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return ItemInvUtil.extractSingle(getBackingInv(), slot, filter, ItemStack.EMPTY, maxAmount, simulation);
    }

    @Override
    public final ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return ItemInvUtil.insertSingle(getBackingInv(), slot, stack, simulation);
    }

    @Override
    public final ItemFilter getInsertionFilter() {
        return getBackingInv().getFilterForSlot(slot);
    }
}
