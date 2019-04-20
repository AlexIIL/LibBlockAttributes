package alexiil.mc.lib.attributes.fluid.impl;

import java.util.ArrayList;
import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public final class CombinedFluidInsertable implements FluidInsertable {

    private final List<? extends FluidInsertable> insertables;

    public CombinedFluidInsertable(List<? extends FluidInsertable> list) {
        this.insertables = list;
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume stack, Simulation simulation) {
        for (FluidInsertable insertable : insertables) {
            stack = insertable.attemptInsertion(stack, simulation);
            if (stack.isEmpty()) {
                return FluidKeys.EMPTY.withAmount(0);
            }
        }
        return stack;
    }

    @Override
    public FluidFilter getInsertionFilter() {
        List<FluidFilter> filters = new ArrayList<>(insertables.size());
        for (int i = 0; i < insertables.size(); i++) {
            filters.add(insertables.get(i).getInsertionFilter());
        }
        return AggregateFluidFilter.allOf(filters);
    }
}
