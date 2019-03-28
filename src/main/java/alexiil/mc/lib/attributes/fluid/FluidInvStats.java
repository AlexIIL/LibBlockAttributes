package alexiil.mc.lib.attributes.fluid;

import java.util.Set;

import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

///** Provides general statistics for any permanent fluid storing inventory. The inventory in question doesn't have to be
// * an {@link IFixedFluidInvView}. */
public interface FluidInvStats {

    FluidInvStatistic getStatistics(FluidFilter filter);

    /** @return A count of all the {@link FluidKey}'s that match the given filter. */
    default int getAmount(FluidFilter filter) {
        return getStatistics(filter).amount;
    }

    /** @return a set containing all of the {@link FluidKey}'s that are stored in the inventory.*/
    Set<FluidKey> getStoredFluids();

    /** Statistics associated with a single {@link FluidFilter} in a given inventory. */
    public static final class FluidInvStatistic {

        public final FluidFilter filter;

        /** The total amount of the given filter. */
        public final int amount;

        /** The total amount of space that the given filter can be added to (where an inventory already contains a
         * partial stack). */
        public final int spaceAddable;

        /** The total amount of additional entries that could be added to by this filter. This might be -1 if the filter
         * isn't specific enough to properly calculate this value. */
        public final int spaceTotal;

        public FluidInvStatistic(FluidFilter filter, int amount, int spaceAddable, int spaceTotal) {
            this.filter = filter;
            this.amount = amount;
            this.spaceAddable = spaceAddable;
            this.spaceTotal = spaceTotal;
        }
    }
}
