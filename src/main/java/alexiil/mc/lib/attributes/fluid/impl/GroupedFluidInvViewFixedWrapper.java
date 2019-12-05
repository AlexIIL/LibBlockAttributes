/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.util.HashSet;
import java.util.Set;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.FluidInvAmountChangeListener_F;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
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
        FluidAmount amount = FluidAmount.ZERO;
        FluidAmount space = FluidAmount.ZERO;
        FluidAmount totalSpace = FluidAmount.ZERO;
        boolean totalSpaceValid = true;
        for (int t = 0; t < inv.getTankCount(); t++) {
            FluidAmount max = inv.getMaxAmount_F(t);
            FluidVolume fluid = inv.getInvFluid(t);
            if (!fluid.isEmpty()) {
                if (filter.matches(fluid.fluidKey)) {
                    amount = amount.add(fluid.getAmount_F());
                    space = space.add(max.sub(fluid.getAmount_F()));
                }
                continue;
            }
            if (FluidFilterUtil.hasIntersection(filter, inv.getFilterForTank(t))) {
                totalSpace = totalSpace.add(max);
            }
        }
        return new FluidInvStatistic(filter, amount, space, totalSpaceValid ? totalSpace : FluidAmount.NEGATIVE_ONE);
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
    public FluidAmount getTotalCapacity_F() {
        FluidAmount total = FluidAmount.ZERO;
        for (int t = 0; t < inv.getTankCount(); t++) {
            total = total.add(inv.getMaxAmount_F(t));
        }
        return total;
    }

    @Override
    public ListenerToken addListener_F(FluidInvAmountChangeListener_F listener, ListenerRemovalToken removalToken) {
        return inv.addListener((i, tank, previous, current) -> {
            if (previous.isEmpty()) {
                if (current.isEmpty()) {
                    // No changes: don't propagate
                } else {
                    FluidAmount currentAmount = this.getAmount_F(current.fluidKey);
                    listener.onChange(this, current.fluidKey, currentAmount.sub(current.getAmount_F()), currentAmount);
                }
            } else {
                if (current.isEmpty()) {
                    FluidAmount previousAmount = this.getAmount_F(previous.fluidKey);
                    FluidAmount prev = previousAmount.add(previous.getAmount_F());
                    listener.onChange(this, previous.fluidKey, prev, previousAmount);
                } else {
                    if (previous.fluidKey == current.fluidKey) {
                        FluidAmount currentAmount = this.getAmount_F(current.fluidKey);
                        FluidAmount diff = current.getAmount_F().sub(previous.getAmount_F());
                        listener.onChange(this, current.fluidKey, currentAmount.sub(diff), currentAmount);
                    } else {
                        FluidAmount currentAmount = this.getAmount_F(current.fluidKey);
                        FluidAmount previousAmount = this.getAmount_F(previous.fluidKey);

                        FluidAmount newPrev = currentAmount.sub(current.getAmount_F());
                        listener.onChange(this, current.fluidKey, newPrev, currentAmount);

                        FluidAmount oldPrev = previousAmount.add(previous.getAmount_F());
                        listener.onChange(this, previous.fluidKey, oldPrev, previousAmount);
                    }
                }
            }
        }, removalToken);
    }
}
