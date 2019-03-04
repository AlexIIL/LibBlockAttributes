package alexiil.mc.lib.attributes.fluid.impl;

import java.util.ArrayList;
import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolume;
import alexiil.mc.lib.attributes.fluid.IFluidInsertable;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;

public final class CombinedFluidInsertable implements IFluidInsertable {

    private final List<? extends IFluidInsertable> insertables;
    private final IFluidFilter filter;

    public CombinedFluidInsertable(List<? extends IFluidInsertable> list) {
        List<IFluidFilter> filters = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            filters.add(list.get(i).getInsertionFilter());
        }
        this.filter = AggregateFluidFilter.allOf(filters);
        this.insertables = list;
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume stack, Simulation simulation) {
        for (IFluidInsertable insertable : insertables) {
            stack = insertable.attemptInsertion(stack, simulation);
            if (stack.isEmpty()) {
                return new FluidVolume();
            }
        }
        return stack;
    }

    @Override
    public IFluidFilter getInsertionFilter() {
        return filter;
    }
}
