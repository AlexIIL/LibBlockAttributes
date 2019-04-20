package alexiil.mc.lib.attributes.fluid.impl;

import java.util.HashSet;
import java.util.Set;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.FluidInvAmountChangeListener;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilterUtil;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class GroupedFluidInvViewFixedWrapper implements GroupedFluidInvView {

    private final FixedFluidInvView inv;

    public GroupedFluidInvViewFixedWrapper(FixedFluidInvView inv) {
        this.inv = inv;
    }

    protected FixedFluidInvView inv() {
        return inv;
    }

    @Override
    public FluidInvStatistic getStatistics(FluidFilter filter) {
        int amount = 0;
        int space = 0;
        int totalSpace = 0;
        boolean totalSpaceValid = true;
        for (int t = 0; t < inv.getTankCount(); t++) {
            int max = inv.getMaxAmount(t);
            FluidVolume fluid = inv.getInvFluid(t);
            if (!fluid.isEmpty()) {
                if (filter.matches(fluid.fluidKey)) {
                    amount += fluid.getAmount();
                    space += max - fluid.getAmount();
                }
                continue;
            }
            if (FluidFilterUtil.hasIntersection(filter, inv.getFilterForTank(t))) {
                totalSpace += max;
            }
        }
        return new FluidInvStatistic(filter, amount, space, totalSpaceValid ? totalSpace : -1);
    }

    @Override
    public Set<FluidKey> getStoredFluids() {
        Set<FluidKey> set = new HashSet<>();
        for (int s = 0; s < inv.getTankCount(); s++) {
            FluidVolume fluid = inv.getInvFluid(s);
            if (!fluid.isEmpty()) {
                set.add(fluid.fluidKey);
            }
        }
        return set;
    }

    @Override
    public int getTotalCapacity() {
        int total = 0;
        for (int t = 0; t < inv.getTankCount(); t++) {
            total += inv.getMaxAmount(t);
        }
        return total;
    }

    @Override
    public ListenerToken addListener(FluidInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        return inv.addListener((i, tank, previous, current) -> {
            if (previous.isEmpty()) {
                if (current.isEmpty()) {
                    // No changes: don't propagate
                } else {
                    int currentAmount = this.getAmount(current.fluidKey);
                    listener.onChange(this, current.fluidKey, currentAmount - current.getAmount(), currentAmount);
                }
            } else {
                if (current.isEmpty()) {
                    int previousAmount = this.getAmount(previous.fluidKey);
                    listener.onChange(this, previous.fluidKey, previousAmount + previous.getAmount(), previousAmount);
                } else {
                    if (previous.fluidKey == current.fluidKey) {
                        int currentAmount = this.getAmount(current.fluidKey);
                        int diff = current.getAmount() - previous.getAmount();
                        listener.onChange(this, current.fluidKey, currentAmount - diff, currentAmount);
                    } else {
                        int currentAmount = this.getAmount(current.fluidKey);
                        int previousAmount = this.getAmount(previous.fluidKey);
                        listener.onChange(this, current.fluidKey, currentAmount - current.getAmount(), currentAmount);
                        listener.onChange(this, previous.fluidKey, previousAmount + previous.getAmount(),
                            previousAmount);
                    }
                }
            }
        }, removalToken);
    }
}
