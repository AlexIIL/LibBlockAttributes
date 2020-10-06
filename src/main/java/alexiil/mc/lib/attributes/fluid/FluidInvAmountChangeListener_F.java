/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

@FunctionalInterface
public interface FluidInvAmountChangeListener_F {

    /** @param inv The inventory that changed
     * @param fluid The {@link FluidKey} whose amount changed.
     * @param previous The previous {@link FluidVolume}.
     * @param current The new {@link FluidVolume}. The {@link FluidVolume#getFluidKey()} will either be the empty fluid
     *            key, or equal to the passed {@link FluidKey} . */
    void onChange(GroupedFluidInvView inv, FluidKey fluid, FluidAmount previous, FluidAmount current);

    @Deprecated // (since = "0.6.0", forRemoval = true)
    public static FluidInvAmountChangeListener asOld(FluidInvAmountChangeListener_F listener) {
        return (inv, fluid, previous, current) -> {
            listener.onChange(inv, fluid, FluidAmount.of1620(previous), FluidAmount.of1620(current));
        };
    }
}
