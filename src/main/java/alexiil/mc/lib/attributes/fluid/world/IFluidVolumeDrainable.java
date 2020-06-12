/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.Waterloggable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Similar to {@link FluidDrainable}, but for {@link FluidVolume}'s. Note that you should never call this directly as
 * vanilla {@link Waterloggable} fluids don't implement this! */
public interface IFluidVolumeDrainable {
    FluidVolume tryDrainFluid(WorldAccess world, BlockPos pos, BlockState state, Simulation simulation);
}
