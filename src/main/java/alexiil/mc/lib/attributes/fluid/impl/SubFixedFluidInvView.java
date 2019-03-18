package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.IListenerRemovalToken;
import alexiil.mc.lib.attributes.IListenerToken;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.IFluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A sub-view of an existing {@link IFixedFluidInvView}. */
public class SubFixedFluidInvView<InvType extends IFixedFluidInvView> implements IFixedFluidInvView {

    /** The inventory that is wrapped. */
    protected final InvType inv;

    /** The tanks that we use. */
    private final int fromIndex, toIndex;

    public SubFixedFluidInvView(InvType inv, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                "fromIndex was greater than toIndex! (" + fromIndex + " > " + toIndex + ")");
        }
        this.inv = inv;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /** @return The tank that the internal {@link #inv} should use. */
    protected int getInternalTank(int tank) {
        tank += fromIndex;
        if (tank >= toIndex) {
            throw new IllegalArgumentException("The given tank " + (tank - fromIndex)
                + "is greater than the size of this inventory! (" + getTankCount() + ")");
        }
        return tank;
    }

    @Override
    public int getTankCount() {
        return toIndex - fromIndex;
    }

    @Override
    public FluidVolume getInvFluid(int tank) {
        return inv.getInvFluid(getInternalTank(tank));
    }

    @Override
    public boolean isFluidValidForTank(int tank, FluidKey fluid) {
        return inv.isFluidValidForTank(getInternalTank(tank), fluid);
    }

    @Override
    public IFluidFilter getFilterForTank(int tank) {
        return inv.getFilterForTank(getInternalTank(tank));
    }

    @Override
    public int getMaxAmount(int tank) {
        return inv.getMaxAmount(getInternalTank(tank));
    }

    @Override
    public IFixedFluidInvView getView() {
        if (getClass() == SubFixedFluidInvView.class) {
            return this;
        }
        return IFixedFluidInvView.super.getView();
    }

    @Override
    public IListenerToken addListener(IFluidInvTankChangeListener listener, IListenerRemovalToken removalToken) {
        IFixedFluidInvView wrapper = this;
        return inv.addListener((realInv, tank, previous, current) -> {
            assert realInv == inv;
            if (tank >= fromIndex && tank < toIndex) {
                int exposedTank = tank - fromIndex;
                listener.onChange(wrapper, exposedTank, previous, current);
            }
        }, removalToken);
    }
}
