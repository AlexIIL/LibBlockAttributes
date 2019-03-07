package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;

public enum FluidVolumeUtil {
    ;

    private static final boolean LONG_LOCALISATION = true;
    private static final boolean USE_FULL_NAMES = true;
    private static final boolean FULLY_EXPAND = true;

    /** @param amount The amount in {@link FluidVolume#BASE_UNIT base units} */
    public static String localizeFluidAmount(int amount) {
        if (FULLY_EXPAND) {

        }
        if (LONG_LOCALISATION) {
            if (amount < FluidVolume.BASE_UNIT) {
                return "0";
            }
            // TODO: Actual localisation!
            // (I'd like to copy this almost directly from buildcraft's LocaleUtil.localizeFluid)
            if (amount < FluidVolume.NUGGET) {
                return (amount / (double) FluidVolume.NUGGET) + " Nuggets";
            }
            if (amount < FluidVolume.INGOT) {
                return (amount / (double) FluidVolume.INGOT) + " Ingots";
            }
            return (amount / (double) FluidVolume.BUCKET) + " Buckets";
        } else {
            return amount / (double) FluidVolume.BUCKET + "Buckets";
        }
    }

    /** Attempts to move up to the given maximum number of items from the {@link IFluidExtractable} to the
     * {@link IFluidInsertable}.
     * 
     * @return The number of items moved.
     * @see #move(IFluidExtractable, IFluidInsertable, IFluidFilter, int) */
    public static int move(IFluidExtractable from, IFluidInsertable to, int maximum) {
        return move(from, to, null, maximum);
    }

    /** Attempts to move up to the given maximum amount of fluids from the {@link IFluidExtractable} to the
     * {@link IFluidInsertable}, provided they match the given {@link IItemFilter}.
     * 
     * @return The number of items moved. */
    public static int move(IFluidExtractable from, IFluidInsertable to, IFluidFilter filter, int maximum) {
        IFluidFilter insertionFilter = to.getInsertionFilter();
        if (filter != null && filter != ConstantFluidFilter.ANYTHING) {
            insertionFilter = AggregateFluidFilter.and(insertionFilter, filter);
        }

        FluidVolume extracted = from.attemptExtraction(insertionFilter, maximum, Simulation.SIMULATE);
        if (extracted.isEmpty()) {
            return 0;
        }
        FluidVolume leftover = to.attemptInsertion(extracted, Simulation.ACTION);
        int insertedAmount = extracted.getAmount() - (leftover.isEmpty() ? 0 : leftover.getAmount());
        FluidVolume reallyExtracted = from.attemptExtraction(insertionFilter, insertedAmount, Simulation.ACTION);

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
