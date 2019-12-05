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
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
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
                return FluidVolumeUtil.EMPTY;
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
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        return CombinedFluidExtractable.attemptExtraction(filter, maxAmount, simulation, inventories);
    }
}
