package alexiil.mc.lib.attributes.fluid.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.Waterloggable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Similar to {@link FluidDrainable}, but for {@link FluidVolume}'s. Note that you should never call this directly as
 * vanilla {@link Waterloggable} fluids don't implement this! */
public interface IFluidVolumeDrainable {
    FluidVolume tryDrainFluid(IWorld world, BlockPos pos, BlockState state, Simulation simulation);
}
