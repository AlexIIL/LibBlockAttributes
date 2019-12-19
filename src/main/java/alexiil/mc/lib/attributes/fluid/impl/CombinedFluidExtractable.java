/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.math.RoundingMode;
import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public final class CombinedFluidExtractable implements FluidExtractable {

    private final List<? extends FluidExtractable> list;

    public CombinedFluidExtractable(List<? extends FluidExtractable> list) {
        this.list = list;
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        return attemptExtraction(filter, maxAmount, simulation, list);
    }

    /** Full implementation of {@link #attemptExtraction(FluidFilter, FluidAmount, Simulation)}, that takes an iterable
     * so it's usable by {@link CombinedGroupedFluidInv} as well.
     * 
     * @param iter Every element will be casted to {@link FluidExtractable}, so you should ensure that the passed
     *            iterable is of the correct type! */
    static FluidVolume attemptExtraction(
        FluidFilter filter, FluidAmount maxAmount, Simulation simulation, Iterable<?> iter
    ) {
        if (maxAmount.isNegative()) {
            throw new IllegalArgumentException("maxCount cannot be negative! (was " + maxAmount + ")");
        }
        FluidVolume extracted = FluidVolumeUtil.EMPTY;
        if (maxAmount.isZero()) {
            return extracted;
        }
        for (Object obj : iter) {
            FluidExtractable extractable = (FluidExtractable) obj;
            if (extracted.isEmpty()) {
                extracted = extractable.attemptExtraction(filter, maxAmount, simulation);
                if (extracted.isEmpty()) {
                    continue;
                }
                if (!extracted.getAmount_F().isLessThan(maxAmount)) {
                    return extracted;
                }
                filter = new ExactFluidFilter(extracted.fluidKey);
            } else {
                FluidAmount newMaxAmount = maxAmount.roundedSub(extracted.getAmount_F(), RoundingMode.UP);
                FluidVolume additional = extractable.attemptExtraction(filter, newMaxAmount, simulation);
                if (additional.isEmpty()) {
                    continue;
                }
                extracted = FluidVolume.merge(extracted, additional);
                if (extracted == null) {
                    throw new IllegalStateException("bad FluidExtractable " + extractable.getClass().getName());
                }
                if (!extracted.getAmount_F().isLessThan(maxAmount)) {
                    return extracted;
                }
            }
        }
        return extracted;
    }

    @Override
    public String toString() {
        if (list.isEmpty()) {
            return "CombinedFluidExtractable{}";
        }
        String inner = "\n";
        for (FluidExtractable extractable : list) {
            inner += "  " + extractable + "\n";
        }
        return "CombinedFluidExtractable{" + inner + "}";
    }
}
