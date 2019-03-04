package alexiil.mc.lib.attributes.fluid.impl;

import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolume;
import alexiil.mc.lib.attributes.fluid.IFluidExtractable;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;

public final class CombinedFluidExtractable implements IFluidExtractable {

    private final List<? extends IFluidExtractable> list;

    public CombinedFluidExtractable(List<? extends IFluidExtractable> list) {
        this.list = list;
    }

    @Override
    public FluidVolume attemptExtraction(IFluidFilter filter, int maxAmount, Simulation simulation) {
        if (maxAmount < 0) {
            throw new IllegalArgumentException("maxCount cannot be negative! (was " + maxAmount + ")");
        }
        FluidVolume extracted = new FluidVolume();
        for (IFluidExtractable extractable : list) {
            if (extracted.isEmpty()) {
                extracted = extractable.attemptExtraction(filter, maxAmount, simulation);
                if (extracted.isEmpty()) {
                    continue;
                }
                if (extracted.getAmount() >= maxAmount) {
                    return extracted;
                }
                filter = new ExactFluidFilter(extracted.toKey());
            } else {
                int newMaxCount = maxAmount - extracted.getAmount();
                FluidVolume additional = extractable.attemptExtraction(filter, newMaxCount, simulation);
                if (additional.isEmpty()) {
                    continue;
                }
                if (!FluidVolume.areEqualExceptAmounts(additional, extracted)) {
                    throw new IllegalStateException("bad IFluidExtractable " + extractable.getClass().getName());
                }
                extracted.add(additional.getAmount());
                if (extracted.getAmount() >= maxAmount) {
                    return extracted;
                }
            }
        }
        return extracted;
    }
}
