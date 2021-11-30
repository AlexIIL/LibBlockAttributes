/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.compat.mod;

import alexiil.mc.lib.attributes.fluid.compat.mod.transfer.TransferFluidInvCompatLoader;
import alexiil.mc.lib.attributes.fluid.compat.mod.vanilla.VanillaFluidCompat;

public final class LbaFluidModCompatLoader {
    private LbaFluidModCompatLoader() {}

    public static void load() {
        VanillaFluidCompat.load();
        TransferFluidInvCompatLoader.load();
    }
}
