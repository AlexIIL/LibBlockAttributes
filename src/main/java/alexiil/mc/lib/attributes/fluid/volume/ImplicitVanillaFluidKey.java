/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;

final class ImplicitVanillaFluidKey extends NormalFluidKey {

    public ImplicitVanillaFluidKey(NormalFluidKeyBuilder builder) {
        super(builder);
    }

    @Override
    public ImplicitVanillaFluidVolume withAmount(int amount) {
        return new ImplicitVanillaFluidVolume(this, amount);
    }

    @Override
    public ImplicitVanillaFluidVolume readVolume(CompoundTag tag) {
        return new ImplicitVanillaFluidVolume(this, tag);
    }
}
