/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.fluid.FluidInvAmountChangeListener_F;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.misc.AbstractCombined;

public class CombinedGroupedFluidInvView extends AbstractCombined<GroupedFluidInvView> implements GroupedFluidInvView {

    protected final List<? extends GroupedFluidInvView> inventories;

    public CombinedGroupedFluidInvView(List<? extends GroupedFluidInvView> inventories) {
        super(inventories);
        this.inventories = inventories;
    }

    @Override
    public FluidInvStatistic getStatistics(FluidFilter filter) {
        FluidAmount amount = FluidAmount.ZERO;
        FluidAmount spaceAddable = FluidAmount.ZERO;
        FluidAmount spaceTotal = FluidAmount.ZERO;
        for (GroupedFluidInvView stats : inventories) {
            FluidInvStatistic stat = stats.getStatistics(filter);
            amount = amount.roundedAdd(stat.amount_F);
            spaceAddable = spaceAddable.roundedAdd(stat.spaceAddable_F);
            if (stat.spaceTotal_F.equals(FluidAmount.NEGATIVE_ONE)) {
                spaceTotal = FluidAmount.NEGATIVE_ONE;
            } else {
                spaceTotal = spaceTotal.roundedAdd(stat.spaceTotal_F);
            }
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
    public FluidAmount getTotalCapacity_F() {
        FluidAmount total = FluidAmount.ZERO;
        for (GroupedFluidInvView inv : inventories) {
            total = total.roundedAdd(inv.getTotalCapacity_F(), RoundingMode.DOWN);
        }
        return total;
    }

    @Override
    public ListenerToken addListener_F(FluidInvAmountChangeListener_F listener, ListenerRemovalToken removalToken) {
        final ListenerToken[] tokens = new ListenerToken[inventories.size()];
        final ListenerRemovalToken ourRemToken = new ListenerRemovalToken() {

            boolean hasAlreadyRemoved = false;

            @Override
            public void onListenerRemoved() {
                for (ListenerToken token : tokens) {
                    if (token == null) {
                        // This means we have only half-initialised
                        // (and all of the next tokens must also be null)
                        return;
                    }
                    token.removeListener();
                }
                if (!hasAlreadyRemoved) {
                    hasAlreadyRemoved = true;
                    removalToken.onListenerRemoved();
                }
            }
        };
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = inventories.get(i).addListener_F((inv, fluidKey, previous, current) -> {
                FluidAmount totalCurrent = this.getAmount_F(fluidKey);
                listener.onChange(this, fluidKey, totalCurrent.roundedSub(current).roundedAdd(previous), totalCurrent);
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
