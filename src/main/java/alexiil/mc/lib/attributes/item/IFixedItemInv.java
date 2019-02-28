package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;
import alexiil.mc.lib.attributes.item.impl.SimpleFixedInvExtractable;
import alexiil.mc.lib.attributes.item.impl.SimpleFixedInvInsertable;
import alexiil.mc.lib.attributes.item.impl.SubFixedItemInv;

/** A changeable {@link IFixedItemInvView} that can have it's contents changed. Note that this does not imply that the
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
 * <p>
 * There are several ways of obtaining instances of this from blocks:
 * <ul>
 * <li>{@link ItemInvUtil#getFixedInv(World, BlockPos)}</li>
 * </ul>
 */
public interface IFixedItemInv extends IFixedItemInvView {

    /** Sets the stack in the given slot to the given stack.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         {@link IFixedItemInvView#isItemValidForSlot(int, ItemStack)} test). */
    boolean setInvStack(int slot, ItemStack to, Simulation simulation);

    /** @return An {@link IItemInsertable} for this inventory that will attempt to insert into any of the slots in this
     *         inventory. */
    default IItemInsertable getInsertable() {
        return new SimpleFixedInvInsertable(this, null);
    }

    /** @return An {@link IItemInsertable} for this inventory that will attempt to insert into only the given array of
     *         slots. */
    default IItemInsertable getInsertable(int[] slots) {
        if (slots.length == 0) {
            return RejectingItemInsertable.NULL;
        }
        return new SimpleFixedInvInsertable(this, slots);
    }

    /** @return An {@link IItemExtractable} for this inventory that will attempt to extract from any of the slots in
     *         this inventory. */
    default IItemExtractable getExtractable() {
        return new SimpleFixedInvExtractable(this, null);
    }

    /** @return An {@link IItemExtractable} for this inventory that will attempt to extract from only the given array of
     *         slots. */
    default IItemExtractable getExtractable(int[] slots) {
        if (slots.length == 0) {
            return EmptyItemExtractable.NULL;
        }
        return new SimpleFixedInvExtractable(this, slots);
    }

    @Override
    default IFixedItemInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        return new SubFixedItemInv<>(this, fromIndex, toIndex);
    }
}
