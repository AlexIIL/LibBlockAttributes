/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.world;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public final class FluidWorldUtil {

    private static final Map<Block, IFluidVolumeDrainable> customDrainables = new IdentityHashMap<>();

    public static void registerCustomDrainable(Block block, IFluidVolumeDrainable drainer) {
        customDrainables.put(block, drainer);
    }

    /** Attempts to drain the given block of it's fluid. */
    public static FluidVolume drain(WorldAccess world, BlockPos pos, Simulation simulation) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof IFluidVolumeDrainable) {
            return ((IFluidVolumeDrainable) block).tryDrainFluid(world, pos, state, simulation);
        } else if (block instanceof Waterloggable && state.contains(Properties.WATERLOGGED)) {
            if (state.get(Properties.WATERLOGGED)) {
                FluidVolume fluidVolume = FluidKeys.WATER.fromWorld(world, pos);
                if (simulation == Simulation.ACTION) {
                    world.setBlockState(pos, state.with(Properties.WATERLOGGED, false), 3);
                }
                return fluidVolume;
            }
            return FluidVolumeUtil.EMPTY;
        } else if (block instanceof FluidBlock && state.contains(Properties.LEVEL_15)) {
            if (state.get(Properties.LEVEL_15) == 0) {
                FluidKey fluidKey = FluidKeys.get(((IFluidBlockMixin) block).__fluid());
                if (fluidKey == null) {
                    return FluidVolumeUtil.EMPTY;
                }
                FluidVolume fluidVolume = fluidKey.fromWorld(world, pos);
                if (simulation == Simulation.ACTION) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                }
                return fluidVolume;
            }
        }
        IFluidVolumeDrainable drainer = customDrainables.get(block);
        if (drainer != null) {
            return drainer.tryDrainFluid(world, pos, state, simulation);
        }
        return FluidVolumeUtil.EMPTY;
    }
}
