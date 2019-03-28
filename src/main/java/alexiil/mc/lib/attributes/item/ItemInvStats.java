package alexiil.mc.lib.attributes.item;

import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.filter.ItemFilter;

import it.unimi.dsi.fastutil.Hash.Strategy;

/** Provides general statistics for any permanent inventory. The inventory in question doesn't have to be an
 * {@link FixedItemInvView}. */
public interface ItemInvStats {

    ItemInvStatistic getStatistics(ItemFilter filter);

    /** @return A count of all the {@link ItemStack}'s that match the given filter. */
    default int getAmount(ItemFilter filter) {
        return getStatistics(filter).amount;
    }

    /** @return a set containing all of the {@link ItemStack}'s that are stored in the inventory. NOTE: This must return
     *         a set using one of the {@link Strategy}'s in {@link ItemStackCollections} otherwise comparison methods
     *         won't work correctly! */
    Set<ItemStack> getStoredStacks();

    /** Statistics associated with a single {@link ItemFilter} in a given inventory. */
    public static final class ItemInvStatistic {

        public final ItemFilter filter;

        /** The total amount of the given filter. */
        public final int amount;

        /** The total amount of space that the given filter can be added to (where an inventory already contains a
         * partial stack). */
        public final int spaceAddable;

        /** The total amount of additional entries that could be added to by this filter. This might be -1 if the filter
         * isn't specific enough to properly calculate this value. */
        public final int spaceTotal;

        public ItemInvStatistic(ItemFilter filter, int amount, int spaceAddable, int spaceTotal) {
            this.filter = filter;
            this.amount = amount;
            this.spaceAddable = spaceAddable;
            this.spaceTotal = spaceTotal;
        }
    }
}
