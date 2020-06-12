/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.Block;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;

import alexiil.mc.lib.attributes.fluid.world.IFluidBlockMixin;

@Mixin(FluidBlock.class)
public class FluidBlockMixin extends Block implements IFluidBlockMixin {
    @Shadow
    protected FlowableFluid fluid;

    public FluidBlockMixin(Settings block$Settings_1) {
        super(block$Settings_1);
    }

    @Override
    public FlowableFluid __fluid() {
        return fluid;
    }
}
