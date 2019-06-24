/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.util.ArrayList;
import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class CombinedGroupedFluidInv extends CombinedGroupedFluidInvView implements GroupedFluidInv {

    public CombinedGroupedFluidInv(List<? extends GroupedFluidInv> inventories) {
        super(inventories);
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        for (GroupedFluidInvView invView : inventories) {
            GroupedFluidInv inv = (GroupedFluidInv) invView;
            fluid = inv.attemptInsertion(fluid, simulation);
            if (fluid.isEmpty()) {
                return FluidKeys.EMPTY.withAmount(0);
            }
        }
        return fluid;
    }

    @Override
    public FluidFilter getInsertionFilter() {
        List<FluidFilter> filters = new ArrayList<>(inventories.size());
        for (int i = 0; i < inventories.size(); i++) {
            filters.add(((GroupedFluidInv) inventories.get(i)).getInsertionFilter());
        }
        return AggregateFluidFilter.allOf(filters);
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
        if (maxAmount < 0) {
            throw new IllegalArgumentException("maxCount cannot be negative! (was " + maxAmount + ")");
        }
        FluidVolume extracted = FluidKeys.EMPTY.withAmount(0);
        for (GroupedFluidInvView invView : inventories) {
            GroupedFluidInv extractable = (GroupedFluidInv) invView;
            if (extracted.isEmpty()) {
                extracted = extractable.attemptExtraction(filter, maxAmount, simulation);
                if (extracted.isEmpty()) {
                    continue;
                }
                if (extracted.getAmount() >= maxAmount) {
                    return extracted;
                }
                filter = new ExactFluidFilter(extracted.fluidKey);
            } else {
                int newMaxCount = maxAmount - extracted.getAmount();
                FluidVolume additional = extractable.attemptExtraction(filter, newMaxCount, simulation);
                if (additional.isEmpty()) {
                    continue;
                }
                extracted = FluidVolume.merge(extracted, additional);
                if (extracted == null) {
                    throw new IllegalStateException("bad FluidExtractable " + extractable.getClass().getName());
                }
                if (extracted.getAmount() >= maxAmount) {
                    return extracted;
                }
            }
        }
        return extracted;
    }
}
