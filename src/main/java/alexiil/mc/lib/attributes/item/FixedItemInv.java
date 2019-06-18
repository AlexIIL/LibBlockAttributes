package alexiil.mc.lib.attributes.item;

import java.util.function.Function;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.GroupedItemInvFixedWrapper;
import alexiil.mc.lib.attributes.item.impl.ItemInvModificationTracker;
import alexiil.mc.lib.attributes.item.impl.MappedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.SimpleLimitedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.SubFixedItemInv;

/** A changeable {@link FixedItemInvView} that can have it's contents changed. Note that this does not imply that the
 * contents can be changed to anything the caller wishes them to be, as implementations can limit the valid
 * {@link ItemStack}s allowed.
 * <p>
 * The attribute is stored in {@link ItemAttributes#FIXED_INV}.
 * <p>
 * <p>
 * There are various classes of interest:
 * <ul>
 * <li>The null instance is {@link EmptyFixedItemInv}</li>
 * <li>A combined view of several sub-inventories is {@link CombinedFixedItemInv}.</li>
 * </ul>
 */
public interface FixedItemInv extends FixedItemInvView {

    /** Sets the stack in the given slot to the given stack.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         {@link FixedItemInvView#isItemValidForSlot(int, ItemStack)} test). */
    boolean setInvStack(int slot, ItemStack to, Simulation simulation);

    /** Sets the stack in the given slot to the given stack, or throws an exception if it was not permitted. */
    default void forceSetInvStack(int slot, ItemStack to) {
        if (!setInvStack(slot, to, Simulation.ACTION)) {
            throw new IllegalStateException(
                "Unable to force-set the slot " + slot + " to " + ItemInvModificationTracker.stackToFullString(to) + "!"
            );
        }
    }

    /** Applies the given function to the stack held in the slot, and uses {@link #forceSetInvStack(int, ItemStack)} on
     * the result (Which will throw an exception if the returned stack is not valid for this inventory). */
    default void modifySlot(int slot, Function<ItemStack, ItemStack> function) {
        forceSetInvStack(slot, function.apply(getInvStack(slot)));
    }

    @Override
    default SingleItemSlot getSlot(int slot) {
        return new SingleItemSlot(this, slot);
    }

    /** @return A new {@link LimitedFixedItemInv} that provides a more controllable version of this
     *         {@link FixedItemInv}. */
    default LimitedFixedItemInv createLimitedInv() {
        return new SimpleLimitedFixedItemInv(this);
    }

    /* Although getGroupedItemInv() makes get{Insertable,Extractable,Transferable} all redundant, it's quite helpful to
     * be able to call the method name that matches what you want to do with it. */

    /** @return An {@link ItemInsertable} for this inventory that will attempt to insert into any of the slots in this
     *         inventory. The default implementation delegates to {@link #getGroupedInv()}. */
    default ItemInsertable getInsertable() {
        return getGroupedInv();
    }

    /** @return An {@link ItemExtractable} for this inventory that will attempt to extract from any of the slots in this
     *         inventory. The default implementation delegates to {@link #getGroupedInv()}. */
    default ItemExtractable getExtractable() {
        return getGroupedInv();
    }

    /** @return An {@link ItemTransferable} for this inventory. The default implementation delegates to
     *         {@link #getGroupedInv()}. */
    default ItemTransferable getTransferable() {
        return getGroupedInv();
    }

    /** @return A {@link GroupedItemInv} for this inventory. The returned value must always be valid for the lifetime of
     *         this {@link FixedItemInv} object. (In other words it must always be valid to cache this returned value
     *         and use it alongside a cached instance of this object). */
    @Override
    default GroupedItemInv getGroupedInv() {
        return new GroupedItemInvFixedWrapper(this);
    }

    @Override
    default FixedItemInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        return new SubFixedItemInv(this, fromIndex, toIndex);
    }

    @Override
    default FixedItemInv getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedItemInv.INSTANCE;
        }
        return new MappedFixedItemInv(this, slots);
    }
}
