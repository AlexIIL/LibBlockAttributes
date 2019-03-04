package alexiil.mc.lib.attributes.item;

import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInvView;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.SimpleFixedItemInvStats;
import alexiil.mc.lib.attributes.item.impl.SubFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.SubFixedItemInvView;

/** A view of a fixed inventory for items, where the size of the inventory never changes, and every slot is "simple":
 * <ul>
 * <li>The amount of every slot will never exceed 64, the normal maximum stack size of minecraft.</li>
 * <li>The stack will stay in the slot until it is removed or changed by something else. (So setting the stack in a slot
 * of an {@link IFixedItemInv} will reflect that change in {@link #getInvStack(int)}).</li>
 * </ul>
 * <p>
 * The attribute is stored in {@link ItemAttributes#FIXED_INV_VIEW}.
 * <p>
 * There are various classes of interest:
 * <ul>
 * <li>A modifiable version of this is {@link IFixedItemInv}.</li>
 * <li>The null instance is {@link EmptyFixedItemInv}</li>
 * <li>A combined view of several sub-inventories is {@link CombinedFixedItemInvView}.</li>
 * <li>A partial view of a single inventory is {@link SubFixedItemInv}</li>
 * </ul>
 */
public interface IFixedItemInvView {

    /** @return The number of slots in this inventory. */
    int getInvSize();

    /** @param slot The slot index. Must be a value between 0 (inclusive) and {@link #getInvSize()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return The ItemStack that is held in the inventory at the moment. The returned stack must never be modified!
     * @throws RuntimeException if the given slot wasn't a valid index. */
    ItemStack getInvStack(int slot);

    /** @param slot The slot index. Must be a value between 0 (inclusive) and {@link #getInvSize()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @param stack The stack to check for. May be an {@link ItemStack#isEmpty() empty} stack.
     * @return The maximum amount that the given slot can hold of the given stack. This method will ignore the current
     *         stack in {@link #getInvStack(int)}. The default implementation just delegates to
     *         {@link ItemStack#getMaxAmount()}. Note that any setters that this object implements (like
     *         {@link IFixedItemInv#setInvStack(int, ItemStack, Simulation)} should reject stacks that are greater than
     *         this value. (and callers should only call this if they need to check the amounts separately. Note that it
     *         is meaningless to return values greater than the maximum amount an item can be stacked to here, and
     *         callers are free to throw an exception if this is violated. (Basically huge single-slot inventories
     *         shouldn't implement this interface).
     * @throws RuntimeException if the given slot wasn't a valid index. */
    default int getMaxAmount(int slot, ItemStack stack) {
        return stack.isEmpty() ? 64 : stack.getMaxAmount();
    }

    /** Checks to see if the given stack is valid for a given slot. This ignores any current stacks in the slot.
     * 
     * @param slot The slot index. Must be a value between 0 (inclusive) and {@link #getInvSize()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @throws RuntimeException if the given slot wasn't a valid index. */
    boolean isItemValidForSlot(int slot, ItemStack stack);

    /** @param slot The slot index. Must be a value between 0 (inclusive) and {@link #getInvSize()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return An {@link IItemFilter} for this slot. If this slot is filtered by an {@link IItemFilter} internally then
     *         it is highly recommended that this be overridden to return *that* filter rather than a newly constructed
     *         one.
     * @throws RuntimeException if the given slot wasn't a valid index. */
    default IItemFilter getFilterForSlot(int slot) {
        return stack -> isItemValidForSlot(slot, stack);
    }

    /** @return A statistical view of this inventory. */
    default IItemInvStats getStatistics() {
        return new SimpleFixedItemInvStats(this);
    }

    /** Adds the given listener to this inventory, such that the
     * {@link IItemInvSlotChangeListener#onChange(IFixedItemInvView, int, ItemStack, ItemStack)} will be called every time
     * that this inventory changes. However if this inventory doesn't support listeners then this will return a null
     * {@link IListenerToken token}.
     * 
     * @return A token that represents the listener, or null if the listener could not be added. */
    IListenerToken addListener(IItemInvSlotChangeListener listener);

    /** Equivalent to {@link List#subList(int, int)}.
     * 
     * @param fromIndex The first slot to expose
     * @param toIndex The slot after the last slot to expose.
     * @return a view of this inventory that only exposes the given number of slots.
     * @throws RuntimeException if any of the given slots weren't valid. */
    default IFixedItemInvView getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        return new SubFixedItemInvView<>(this, fromIndex, toIndex);
    }

    /** @return An object that only implements {@link IFixedItemInvView}, and does not expose the modification methods
     *         that {@link IFixedItemInv} does. Implementations that don't expose any modification methods themselves
     *         should override this method to just return themselves. */
    default IFixedItemInvView getView() {
        final IFixedItemInvView real = this;
        return new IFixedItemInvView() {
            @Override
            public int getInvSize() {
                return real.getInvSize();
            }

            @Override
            public ItemStack getInvStack(int slot) {
                return real.getInvStack(slot);
            }

            @Override
            public boolean isItemValidForSlot(int slot, ItemStack item) {
                return real.isItemValidForSlot(slot, item);
            }

            @Override
            public int getMaxAmount(int slot, ItemStack stack) {
                return real.getMaxAmount(slot, stack);
            }

            @Override
            public IItemFilter getFilterForSlot(int slot) {
                return real.getFilterForSlot(slot);
            }

            @Override
            public IItemInvStats getStatistics() {
                return new SimpleFixedItemInvStats(this);
            }

            @Override
            public IListenerToken addListener(IItemInvSlotChangeListener listener) {
                final IFixedItemInvView view = this;
                return real.addListener((inv, slot, prev, curr) -> {
                    // Defend against giving the listener the real (possibly changeable) inventory.
                    // In addition the listener would probably cache *this view* rather than the backing inventory
                    // so they most likely need it to be this inventory.
                    listener.onChange(view, slot, prev, curr);
                });
            }
        };
    }

    public interface IListenerToken {
        /** Removes the listener from the {@link IFixedItemInv}. */
        void removeListener();
    }
}
