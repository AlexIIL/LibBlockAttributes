/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.fluid.BaseFluid;
import net.minecraft.world.WorldView;

@Mixin(BaseFluid.class)
public interface BaseFluidAccessor {
    @Invoker("getLevelDecreasePerBlock")
    int lba_getLevelDecrease(WorldView worldView);
}
