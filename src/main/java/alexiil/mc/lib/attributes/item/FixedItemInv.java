package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;
import alexiil.mc.lib.attributes.item.impl.SimpleFixedItemInvExtractable;
import alexiil.mc.lib.attributes.item.impl.SimpleFixedItemInvInsertable;
import alexiil.mc.lib.attributes.item.impl.SubFixedItemInv;

/** A changeable {@link FixedItemInvView} that can have it's contents changed. Note that this does not imply that the
 * contents can be changed to anything the caller wishes them to be.
 * <p>
 * The attribute is stored in {@link ItemAttributes#FIXED_INV}.
 * <p>
 * <p>
 * There are various classes of interest:
 * <ul>
 * <li>The null instance is {@link EmptyFixedItemInv}</li>
 * <li>A combined view of several sub-inventories is {@link CombinedFixedItemInv}.</li>
 * <li>A partial view of a single inventory is {@link SubFixedItemInv}</li>
 * </ul>
 */
public interface FixedItemInv extends FixedItemInvView {

    /** Sets the stack in the given slot to the given stack.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         {@link FixedItemInvView#isItemValidForSlot(int, ItemStack)} test). */
    boolean setInvStack(int slot, ItemStack to, Simulation simulation);

    /** @return An {@link ItemInsertable} for this inventory that will attempt to insert into any of the slots in this
     *         inventory. */
    default ItemInsertable getInsertable() {
        return new SimpleFixedItemInvInsertable(this, null);
    }

    /** @return An {@link ItemInsertable} for this inventory that will attempt to insert into only the given array of
     *         slots. */
    default ItemInsertable getInsertable(int[] slots) {
        if (slots.length == 0) {
            return RejectingItemInsertable.NULL;
        }
        return new SimpleFixedItemInvInsertable(this, slots);
    }

    /** @return An {@link ItemExtractable} for this inventory that will attempt to extract from any of the slots in
     *         this inventory. */
    default ItemExtractable getExtractable() {
        return new SimpleFixedItemInvExtractable(this, null);
    }

    /** @return An {@link ItemExtractable} for this inventory that will attempt to extract from only the given array of
     *         slots. */
    default ItemExtractable getExtractable(int[] slots) {
        if (slots.length == 0) {
            return EmptyItemExtractable.NULL;
        }
        return new SimpleFixedItemInvExtractable(this, slots);
    }

    @Override
    default FixedItemInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        return new SubFixedItemInv<>(this, fromIndex, toIndex);
    }
}
