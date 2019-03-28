package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.FluidInvStats;
import alexiil.mc.lib.attributes.fluid.FluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link FixedFluidInv} with no tanks. Because this inventory is unmodifiable this also doubles as the empty
 * implementation for {@link FixedFluidInvView}. */
public enum EmptyFixedFluidInv implements FixedFluidInv {
    INSTANCE;

    private static IllegalArgumentException throwInvalidTankException() {
        throw new IllegalArgumentException("There are no valid tanks in this empty inventory!");
    }

    @Override
    public int getTankCount() {
        return 0;
    }

    @Override
    public FluidVolume getInvFluid(int tank) {
        throw throwInvalidTankException();
    }

    @Override
    public boolean isFluidValidForTank(int tank, FluidKey item) {
        throw throwInvalidTankException();
    }

    @Override
    public FluidFilter getFilterForTank(int tank) {
        throw throwInvalidTankException();
    }

    @Override
    public int getMaxAmount(int tank) {
        throw throwInvalidTankException();
    }

    @Override
    public FluidInvStats getStatistics() {
        return EmptyFluidInvStats.INSTANCE;
    }

    @Override
    public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
        // We don't need to keep track of the listener because this empty inventory never changes.
        return () -> {
            // (And we don't need to do anything when the listener is removed)
        };
        // Never call the removal token as it's unnecessary (and saves the caller from re-adding it every tick)
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        throw throwInvalidTankException();
    }

    @Override
    public FixedFluidInvView getView() {
        return this;
    }

    @Override
    public FluidInsertable getInsertable() {
        return RejectingFluidInsertable.NULL;
    }

    @Override
    public FluidInsertable getInsertable(int[] tanks) {
        if (tanks.length == 0) {
            return RejectingFluidInsertable.NULL;
        }
        throw throwInvalidTankException();
    }

    @Override
    public FluidExtractable getExtractable() {
        return EmptyFluidExtractable.NULL;
    }

    @Override
    public FluidExtractable getExtractable(int[] tanks) {
        if (tanks.length == 0) {
            return EmptyFluidExtractable.NULL;
        }
        throw throwInvalidTankException();
    }
}
