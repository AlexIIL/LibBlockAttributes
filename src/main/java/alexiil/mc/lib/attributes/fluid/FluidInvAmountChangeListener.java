/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Replaced by {@link FluidInvAmountChangeListener_F}. */
@FunctionalInterface
@Deprecated // (since = "0.6.0", forRemoval = true)
public interface FluidInvAmountChangeListener {

    /** @param inv The inventory that changed
     * @param fluid The {@link FluidKey} whose amount changed.
     * @param previous The previous {@link FluidVolume}.
     * @param current The new {@link FluidVolume}. The {@link FluidVolume#getFluidKey()} will either be the empty fluid
     *            key, or equal to the passed {@link FluidKey} . */
    void onChange(GroupedFluidInvView inv, FluidKey fluid, int previous, int current);

    public static FluidInvAmountChangeListener_F asNew(FluidInvAmountChangeListener old) {
        return (inv, fluid, previous, current) -> old.onChange(inv, fluid, previous.as1620(), current.as1620());
    }
}
