package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.attributes.fluid.volume.NormalFluidKey.SimpleFluidKey;

/** {@link ItemStack} equivalent for {@link Fluid fluids}. */
public abstract class NormalFluidVolume extends FluidVolume {

    public NormalFluidVolume(NormalFluidKey fluid, int amount) {
        super(fluid, amount);
        if (fluid.fluid instanceof EmptyFluid && fluid != FluidKeys.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (fluid.fluid instanceof BaseFluid && fluid.fluid != ((BaseFluid) fluid.fluid).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
    }

    public NormalFluidVolume(NormalFluidKey fluid, CompoundTag tag) {
        super(fluid, tag);
        if (fluid == null) {
            throw new NullPointerException("fluid");
        }
        if (fluid.fluid instanceof EmptyFluid && fluid != FluidKeys.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (fluid.fluid instanceof BaseFluid && fluid.fluid != ((BaseFluid) fluid.fluid).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
    }

    public final Fluid getRawFluid() {
        return getFluidKey().fluid;
    }

    @Override
    public NormalFluidKey getFluidKey() {
        return (NormalFluidKey) fluidKey;
    }

    /** A simple fluid volume that doesn't store any additional data for the fluid it holds. */
    public static final class SimpleFluidVolume extends NormalFluidVolume {

        public SimpleFluidVolume(SimpleFluidKey fluid, int amount) {
            super(fluid, amount);
        }

        public SimpleFluidVolume(SimpleFluidKey fluid, CompoundTag tag) {
            super(fluid, tag);
        }

        @Override
        public SimpleFluidKey getFluidKey() {
            return (SimpleFluidKey) fluidKey;
        }

        @Override
        public Identifier getSprite() {
            return getFluidKey().spriteId;
        }

        @Override
        public int getRenderColor() {
            return getFluidKey().renderColor;
        }
    }
}
