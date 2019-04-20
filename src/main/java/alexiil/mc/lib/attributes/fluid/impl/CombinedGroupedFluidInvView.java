package alexiil.mc.lib.attributes.fluid.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.fluid.FluidInvAmountChangeListener;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.misc.BoolRef;

public class CombinedGroupedFluidInvView implements GroupedFluidInvView {

    protected final List<? extends GroupedFluidInvView> inventories;

    public CombinedGroupedFluidInvView(List<? extends GroupedFluidInvView> inventories) {
        this.inventories = inventories;
    }

    @Override
    public FluidInvStatistic getStatistics(FluidFilter filter) {
        int amount = 0;
        int spaceAddable = 0;
        int spaceTotal = 0;
        for (GroupedFluidInvView stats : inventories) {
            FluidInvStatistic stat = stats.getStatistics(filter);
            amount += stat.amount;
            spaceAddable += stat.spaceAddable;
            spaceTotal += stat.spaceTotal;
        }
        return new FluidInvStatistic(filter, amount, spaceAddable, spaceTotal);
    }

    @Override
    public Set<FluidKey> getStoredFluids() {
        Set<FluidKey> set = new HashSet<>();
        for (GroupedFluidInvView stats : inventories) {
            set.addAll(stats.getStoredFluids());
        }
        return set;
    }

    @Override
    public int getTotalCapacity() {
        int total = 0;
        for (GroupedFluidInvView inv : inventories) {
            total += inv.getTotalCapacity();
        }
        return total;
    }

    @Override
    public ListenerToken addListener(FluidInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        final ListenerToken[] tokens = new ListenerToken[inventories.size()];
        final BoolRef hasAlreadyRemoved = new BoolRef(false);
        final ListenerRemovalToken ourRemToken = () -> {
            for (ListenerToken token : tokens) {
                if (token == null) {
                    // This means we have only half-initialised
                    // (and all of the next tokens must also be null)
                    return;
                }
                token.removeListener();
            }
            if (!hasAlreadyRemoved.value) {
                hasAlreadyRemoved.value = true;
                removalToken.onListenerRemoved();
            }

        };
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = inventories.get(i).addListener((inv, fluidKey, previous, current) -> {
                int totalCurrent = this.getAmount(fluidKey);
                listener.onChange(this, fluidKey, totalCurrent - current + previous, totalCurrent);
            }, ourRemToken);
            if (tokens[i] == null) {
                for (int j = 0; j < i; j++) {
                    tokens[j].removeListener();
                }
                return null;
            }
        }
        return () -> {
            for (ListenerToken token : tokens) {
                token.removeListener();
            }
        };
    }
}
