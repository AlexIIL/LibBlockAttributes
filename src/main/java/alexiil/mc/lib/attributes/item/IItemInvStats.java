package alexiil.mc.lib.attributes.item;

import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;
import alexiil.mc.lib.attributes.item.impl.CombinedItemInvStats;
import alexiil.mc.lib.attributes.item.impl.EmptyItemInvStats;

import it.unimi.dsi.fastutil.Hash.Strategy;

/** Provides general statistics for any permanent inventory. The inventory in question doesn't have to be an
 * {@link IFixedItemInvView}. */
public interface IItemInvStats {

    public static final CombinableAttribute<IItemInvStats> ATTRIBUTE_STATS =
        new CombinableAttribute<>(IItemInvStats.class, EmptyItemInvStats.INSTANCE, CombinedItemInvStats::new);

    ItemInvStatistic getStatistics(IItemFilter filter);

    /** @return A count of all the {@link ItemStack}'s that match the given filter. */
    default int getAmount(IItemFilter filter) {
        return getStatistics(filter).amount;
    }

    /** @return a set containing all of the {@link ItemStack}'s that are stored in the inventory. NOTE: This must return
     *         a set using one of the {@link Strategy}'s in {@link ItemStackCollections} otherwise comparison methods
     *         won't work correctly! */
    Set<ItemStack> getStoredStacks();

    /** Statistics associated with a single {@link IItemFilter} in a given inventory. */
    public static final class ItemInvStatistic {

        public final IItemFilter filter;

        /** The total amount of the given filter. */
        public final int amount;

        /** The total amount of space that the given filter can be added to (where an inventory already contains a
         * partial stack). */
        public final int spaceAddable;

        /** The total amount of additional entries that could be added to by this filter. This might be -1 if the filter
         * isn't specific enough to properly calculate this value. */
        public final int spaceTotal;

        public ItemInvStatistic(IItemFilter filter, int amount, int spaceAddable, int spaceTotal) {
            this.filter = filter;
            this.amount = amount;
            this.spaceAddable = spaceAddable;
            this.spaceTotal = spaceTotal;
        }
    }
}
