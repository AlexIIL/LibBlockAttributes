/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A delegating view of a single slot in a {@link FixedFluidInvView}. */
public class SingleFluidTankView {

    final FixedFluidInvView backingView;
    final int tank;

    SingleFluidTankView(FixedFluidInvView backingView, int tank) {
        this.backingView = backingView;
        this.tank = tank;
    }

    public FixedFluidInvView getBackingInv() {
        return backingView;
    }

    public final int getIndex() {
        return tank;
    }

    public final FluidVolume get() {
        return backingView.getInvFluid(tank);
    }

    public final int getMaxAmount() {
        return backingView.getMaxAmount(tank);
    }

    public final boolean isValid(FluidKey fluid) {
        return backingView.isFluidValidForTank(tank, fluid);
    }

    public final FluidFilter getFilter() {
        return backingView.getFilterForTank(tank);
    }

    /** Adds the given listener to the backing inventory, such that the
     * {@link FluidInvTankChangeListener#onChange(FixedFluidInvView, int, FluidVolume, FluidVolume)} will be called
     * every time that this inventory changes. However if this inventory doesn't support listeners then this will return
     * a null {@link ListenerToken token}.
     * 
     * @param removalToken A token that will be called whenever the given listener is removed from this inventory (or if
     *            this inventory itself is unloaded or otherwise invalidated).
     * @return A token that represents the listener, or null if the listener could not be added. */
    public final ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
        return backingView.addListener((realInv, s, previous, current) -> {
            assert realInv == backingView;
            if (tank == s) {
                listener.onChange(realInv, tank, previous, current);
            }
        }, removalToken);
    }
}
