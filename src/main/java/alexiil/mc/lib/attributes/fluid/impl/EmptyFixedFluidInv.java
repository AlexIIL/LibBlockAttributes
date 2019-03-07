package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.IListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.IFluidExtractable;
import alexiil.mc.lib.attributes.fluid.IFluidInsertable;
import alexiil.mc.lib.attributes.fluid.IFluidInvStats;
import alexiil.mc.lib.attributes.fluid.IFluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link IFixedFluidInv} with no tanks. Because this inventory is unmodifiable this also doubles as the empty
 * implementation for {@link IFixedFluidInvView}. */
public enum EmptyFixedFluidInv implements IFixedFluidInv {
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
    public IFluidFilter getFilterForTank(int tank) {
        throw throwInvalidTankException();
    }

    @Override
    public int getMaxAmount(int tank) {
        throw throwInvalidTankException();
    }

    @Override
    public IFluidInvStats getStatistics() {
        return EmptyFluidInvStats.INSTANCE;
    }

    @Override
    public IListenerToken addListener(IFluidInvTankChangeListener listener) {
        // We don't need to keep track of the listener because this empty inventory never changes.
        return () -> {
            // (And we don't need to do anything when the listener is removed)
        };
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        throw throwInvalidTankException();
    }

    @Override
    public IFixedFluidInvView getView() {
        return this;
    }

    @Override
    public IFluidInsertable getInsertable() {
        return RejectingFluidInsertable.NULL;
    }

    @Override
    public IFluidInsertable getInsertable(int[] tanks) {
        if (tanks.length == 0) {
            return RejectingFluidInsertable.NULL;
        }
        throw throwInvalidTankException();
    }

    @Override
    public IFluidExtractable getExtractable() {
        return EmptyFluidExtractable.NULL;
    }

    @Override
    public IFluidExtractable getExtractable(int[] tanks) {
        if (tanks.length == 0) {
            return EmptyFluidExtractable.NULL;
        }
        throw throwInvalidTankException();
    }
}
