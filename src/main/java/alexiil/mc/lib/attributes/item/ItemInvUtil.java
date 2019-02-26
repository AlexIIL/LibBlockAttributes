package alexiil.mc.lib.attributes.item;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.AggregateStackFilter;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemInvStats;
import alexiil.mc.lib.attributes.util.AttributeObtainingImpl;

/** Various hooks and methods for dealing with obtaining {@link IFixedItemInv}, {@link IFixedItemInvView},
 * {@link IItemInsertable}, {@link IItemExtractable}, and {@link IItemInvStats}. */
public enum ItemInvUtil {
    ;

    // #######################
    // Public instance getters
    // #######################
    // All of these just delegate to
    // AttributeObtainingImpl to keep
    // all of the various plumbing
    // methods in one private place.
    // #######################

    /** @param world
     * @param pos
     * @return A combined version of all {@link IFixedItemInv}'s available at the given location, or
     *         {@link EmptyFixedItemInv} if none were found. */
    @Nonnull
    public static IFixedItemInv getFixedInv(World world, BlockPos pos) {
        return AttributeObtainingImpl.getFixedInventory(world, pos);
    }

    /** @param world
     * @param pos
     * @return A combined version of all {@link IFixedItemInv}'s available at the given location, or
     *         {@link EmptyFixedItemInv} if none were found. */
    @Nonnull
    public static IFixedItemInvView getFixedInvView(World world, BlockPos pos) {
        return AttributeObtainingImpl.getFixedInventoryView(world, pos);
    }

    /** @param world
     * @param pos
     * @return A combined version of all {@link IItemInvStats}'s available at the given location, or
     *         {@link EmptyItemInvStats} if none were found. */
    @Nonnull
    public static IItemInvStats getItemInvStats(World world, BlockPos pos) {
        return AttributeObtainingImpl.getItemInventoryStats(world, pos);
    }

    /** @param world
     * @param pos
     * @return A combined version of all {@link IFixedItemInv}'s available at the given location, or
     *         {@link EmptyFixedItemInv} if none were found. */
    @Nonnull
    public static IItemInsertable getInsertable(World world, BlockPos pos, Direction from) {
        return AttributeObtainingImpl.getInsertable(world, pos, from);
    }

    /** @param world
     * @param pos
     * @return A combined version of all {@link IFixedItemInv}'s available at the given location, or
     *         {@link EmptyFixedItemInv} if none were found. */
    @Nonnull
    public static IItemExtractable getExtractable(World world, BlockPos pos, Direction from) {
        return AttributeObtainingImpl.getExtractable(world, pos, from);
    }

    // #######################
    // Direct utility methods
    // #######################
    // Most of these also delegate
    // to various internal classes
    // to do all of the heavy lifting
    // #######################

    /** Attempts to move up to the given maximum number of items from the {@link IItemExtractable} to the
     * {@link IItemInsertable}.
     * 
     * @return The number of items moved.
     * @see #move(IItemExtractable, IItemInsertable, IItemFilter, int) */
    public static int move(IItemExtractable from, IItemInsertable to, int maximum) {
        return move(from, to, null, maximum);
    }

    /** Attempts to move up to the given maximum number of items from the {@link IItemExtractable} to the
     * {@link IItemInsertable}, provided they match the given {@link IItemFilter}.
     * 
     * @return The number of items moved. */
    public static int move(IItemExtractable from, IItemInsertable to, IItemFilter filter, int maximum) {
        IItemFilter insertionFilter = to.getInsertionFilter();
        if (filter != null && filter != IItemFilter.ANY_STACK) {
            insertionFilter = AggregateStackFilter.and(insertionFilter, filter);
        }

        ItemStack extracted = from.attemptExtraction(insertionFilter, maximum, Simulation.SIMULATE);
        if (extracted.isEmpty()) {
            return 0;
        }
        ItemStack leftover = to.attemptInsertion(extracted, Simulation.ACTION);
        int insertedAmount = extracted.getAmount() - (leftover.isEmpty() ? 0 : leftover.getAmount());
        ItemStack reallyExtracted = from.attemptExtraction(insertionFilter, insertedAmount, Simulation.ACTION);

        if (reallyExtracted.isEmpty()) {
            throw throwBadImplException("Tried to extract the filter (C) from A but it returned an empty item stack "
                + "after we have already inserted the expected stack into B!\nThe inventory is now in an invalid (duped) state!",
                new String[] { "from A", "to B", "filter C" }, new Object[] { from, to, filter });
        }
        if (reallyExtracted.getAmount() != insertedAmount) {
            throw throwBadImplException(
                "Tried to extract " + insertedAmount + " but we actually extracted " + reallyExtracted.getAmount()
                    + "!\nThe inventory is now in an invalid (duped) state!",
                new String[] { "from A", "to B", "filter C", "originally extracted", "really extracted" },
                new Object[] { from, to, insertionFilter, extracted, reallyExtracted });
        }
        return insertedAmount;
    }

    private static IllegalStateException throwBadImplException(String reason, String[] names, Object[] objs) {
        String detail = "\n";
        int max = Math.max(names.length, objs.length);
        for (int i = 0; i < max; i++) {
            String name = names.length <= i ? "?" : names[i];
            Object obj = objs.length <= i ? "" : objs[i];
            // TODO: Full object detail!
            detail += "\n" + name + " = " + obj;
        }
        throw new IllegalStateException(reason + detail);
    }
}
