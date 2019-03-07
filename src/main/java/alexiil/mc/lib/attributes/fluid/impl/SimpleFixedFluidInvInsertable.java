package alexiil.mc.lib.attributes.fluid.impl;

import java.util.ArrayList;
import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.IFluidInsertable;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link IFluidInsertable} wrapper over an {@link IFixedFluidInv}. This implementation is the naive implementation
 * where every insertion operation will look at every tank in the target inventory in order to insert into the most
 * appropriate tank first. As such the use of this class is discouraged whenever a more efficient version can be made
 * (unless the target inventory has a very small {@link IFixedFluidInvView#getTankCount() size}. */
public final class SimpleFixedFluidInvInsertable implements IFluidInsertable {

    private final IFixedFluidInv inv;

    /** Null means that this can insert into any of the tanks. */
    private final int[] tanks;

    public SimpleFixedFluidInvInsertable(IFixedFluidInv inv, int[] tanks) {
        this.inv = inv;
        this.tanks = tanks;
    }

    @Override
    public IFluidFilter getInsertionFilter() {
        if (tanks == null) {
            int tankCount = inv.getTankCount();
            switch (tankCount) {
                case 0: {
                    // What?
                    return ConstantFluidFilter.NOTHING;
                }
                case 1: {
                    return inv.getFilterForTank(0);
                }
                case 2: {
                    return inv.getFilterForTank(0).and(inv.getFilterForTank(1));
                }
                default: {
                    List<IFluidFilter> filters = new ArrayList<>(tankCount);
                    for (int i = 0; i < tankCount; i++) {
                        filters.add(inv.getFilterForTank(i));
                    }
                    return AggregateFluidFilter.anyOf(filters);
                }
            }
        } else {
            switch (tanks.length) {
                case 0: {
                    // What?
                    return ConstantFluidFilter.NOTHING;
                }
                case 1: {
                    return inv.getFilterForTank(tanks[0]);
                }
                case 2: {
                    return inv.getFilterForTank(tanks[0]).and(inv.getFilterForTank(tanks[1]));
                }
                default: {
                    List<IFluidFilter> filters = new ArrayList<>(tanks.length);
                    for (int s : tanks) {
                        filters.add(inv.getFilterForTank(s));
                    }
                    return AggregateFluidFilter.anyOf(filters);
                }
            }
        }
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume stack, Simulation simulation) {
        // FluidVolume leftover = stack.copy();
        //
        // // First: scan the available tanks to see if we can add to an existing stack
        //
        // IntList tanksModified = new IntArrayList();
        //
        // if (tanks == null) {
        // for (int s = 0; s < inv.getInvSize(); s++) {
        // attemptAddToExisting(tanksModified, s, leftover, simulation);
        // }
        // } else {
        // for (int s : tanks) {
        // attemptAddToExisting(tanksModified, s, leftover, simulation);
        // }
        // }

        return simpleDumbBadInsertionToBeRemoved(stack, simulation);
    }

    private FluidVolume simpleDumbBadInsertionToBeRemoved(FluidVolume fluid, Simulation simulation) {
        fluid = fluid.copy();
        if (tanks == null) {
            for (int t = 0; t < inv.getTankCount(); t++) {
                FluidVolume inTank = inv.getInvFluid(t);
                int current = inTank.isEmpty() ? 0 : inTank.getAmount();
                int max = Math.min(current + fluid.getAmount(), inv.getMaxAmount(t));
                int addable = max - current;
                if (addable <= 0) {
                    continue;
                }
                FluidVolume fluidCopy = fluid.copy();
                FluidVolume fluidAddable = fluidCopy.split(addable);
                FluidVolume merged = FluidVolume.merge(inTank, fluidAddable);

                if (merged != null && inv.setInvFluid(t, merged, simulation)) {
                    fluid = fluidCopy;
                    if (fluid.isEmpty()) {
                        return FluidKeys.EMPTY.withAmount(0);
                    }
                }
            }
        } else {
            for (int t : tanks) {
                // Copy of above
                FluidVolume inTank = inv.getInvFluid(t);
                int current = inTank.isEmpty() ? 0 : inTank.getAmount();
                int max = Math.min(current + fluid.getAmount(), inv.getMaxAmount(t));
                int addable = max - current;
                if (addable <= 0) {
                    continue;
                }
                FluidVolume fluidCopy = fluid.copy();
                FluidVolume fluidAddable = fluidCopy.split(addable);
                FluidVolume merged = FluidVolume.merge(inTank, fluidAddable);

                if (merged != null && inv.setInvFluid(t, merged, simulation)) {
                    fluid = fluidCopy;
                    if (fluid.isEmpty()) {
                        return FluidKeys.EMPTY.withAmount(0);
                    }
                }
            }
        }
        return fluid;
    }
}
