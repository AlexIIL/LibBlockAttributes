/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public interface FluidInvTankChangeListener {

    /** @param inv The inventory that changed
     * @param tank The tank that changed
     * @param previous The previous {@link FluidVolume}.
     * @param current The new {@link FluidVolume} */
    void onChange(FixedFluidInvView inv, int tank, FluidVolume previous, FluidVolume current);
}
