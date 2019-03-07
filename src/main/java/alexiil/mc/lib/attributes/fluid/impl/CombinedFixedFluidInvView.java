package alexiil.mc.lib.attributes.fluid.impl;

import java.util.List;

import alexiil.mc.lib.attributes.IListenerToken;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.IFluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link IFixedFluidInvView} that delegates to a list of them instead of storing items directly. */
public class CombinedFixedFluidInvView<InvType extends IFixedFluidInvView> implements IFixedFluidInvView {

    public final List<? extends InvType> views;
    private final int[] subTankStartIndex;
    private final int invSize;

    public CombinedFixedFluidInvView(List<? extends InvType> views) {
        this.views = views;
        subTankStartIndex = new int[views.size()];
        int size = 0;
        for (int i = 0; i < views.size(); i++) {
            subTankStartIndex[i] = size;
            IFixedFluidInvView view = views.get(i);
            int s = view.getTankCount();
            size += s;
        }
        invSize = size;
    }

    @Override
    public int getTankCount() {
        return invSize;
    }

    protected InvType getInv(int tank) {
        if (tank < 0) {
            throw new IllegalArgumentException("Tank must be non-negative! (was " + tank + ")");
        }

        for (int i = 0; i < subTankStartIndex.length; i++) {
            int startIndex = subTankStartIndex[i];
            if (tank < startIndex) {
                return views.get(i);
            }
        }
        if (tank < invSize) {
            return views.get(views.size() - 1);
        }

        throw new IllegalArgumentException(
            "Tank must be less than getInvSize() (was " + tank + ", maximum tank is " + (invSize - 1) + ")");
    }

    protected int getSubTank(int tank) {
        if (tank < 0) {
            throw new IllegalArgumentException("Tank must be non-negative! (was " + tank + ")");
        }

        for (int i = 0; i < subTankStartIndex.length; i++) {
            int startIndex = subTankStartIndex[i];
            if (tank < startIndex) {
                if (i == 0) {
                    return tank;
                }
                return tank - subTankStartIndex[i - 1];
            }
        }
        if (tank < invSize) {
            return tank - subTankStartIndex[subTankStartIndex.length - 1];
        }

        throw new IllegalArgumentException(
            "Tank must be less than getInvSize() (was " + tank + ", maximum tank is " + (invSize - 1) + ")");
    }

    @Override
    public FluidVolume getInvFluid(int tank) {
        return getInv(tank).getInvFluid(getSubTank(tank));
    }

    @Override
    public boolean isFluidValidForTank(int tank, FluidKey fluid) {
        return getInv(tank).isFluidValidForTank(getSubTank(tank), fluid);
    }

    @Override
    public IFluidFilter getFilterForTank(int tank) {
        return getInv(tank).getFilterForTank(getSubTank(tank));
    }

    @Override
    public int getMaxAmount(int tank) {
        return getInv(tank).getMaxAmount(getSubTank(tank));
    }

    @Override
    public IListenerToken addListener(IFluidInvTankChangeListener listener) {
        IListenerToken[] tokens = new IListenerToken[views.size()];
        for (int i = 0; i < tokens.length; i++) {
            final int index = i;
            tokens[i] = views.get(i).addListener((inv, subTank, previous, current) -> {
                listener.onChange(this, subTankStartIndex[index] + subTank, previous, current);
            });
            if (tokens[i] == null) {
                for (int j = 0; j < i; j++) {
                    tokens[j].removeListener();
                }
                return null;
            }
        }
        return () -> {
            for (IListenerToken token : tokens) {
                token.removeListener();
            }
        };
    }
}
