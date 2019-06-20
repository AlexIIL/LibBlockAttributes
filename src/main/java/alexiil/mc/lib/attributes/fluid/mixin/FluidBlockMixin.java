package alexiil.mc.lib.attributes.fluid.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.Block;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.BaseFluid;

import alexiil.mc.lib.attributes.fluid.world.IFluidBlockMixin;

@Mixin(FluidBlock.class)
public class FluidBlockMixin extends Block implements IFluidBlockMixin {
    @Shadow
    protected BaseFluid fluid;

    public FluidBlockMixin(Settings block$Settings_1) {
        super(block$Settings_1);
    }

    @Override
    public BaseFluid __fluid() {
        return fluid;
    }
}
