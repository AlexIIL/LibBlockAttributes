/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.compat.mod;

import alexiil.mc.lib.attributes.fluid.compat.mod.reborncore.RebornCompatLoader;
import alexiil.mc.lib.attributes.fluid.compat.mod.silk.SilkFluidCompat;
import alexiil.mc.lib.attributes.fluid.compat.mod.vanilla.VanillaFluidCompat;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public final class LbaFluidModCompatLoader {
    private LbaFluidModCompatLoader() {}

    public static void load() {
        VanillaFluidCompat.load();

        try {
            Class.forName("io.github.prospector.silk.fluid.FluidContainerProvider");
            LibBlockAttributes.LOGGER.info("Silk found, loading compatibility for fluids.");
            SilkFluidCompat.load();
        } catch (ClassNotFoundException cnfe) {
            LibBlockAttributes.LOGGER.info("Silk not found, not loading compatibility for fluids.");
        }

        //FIXME: Fix reborn core compat once it updates to 1.17
        //RebornCompatLoader.load();
    }
}
