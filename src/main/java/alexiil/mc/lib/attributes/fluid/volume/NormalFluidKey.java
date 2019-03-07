package alexiil.mc.lib.attributes.fluid.volume;

import javax.annotation.Nonnull;

import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.fluid.volume.NormalFluidVolume.SimpleFluidVolume;

/** Identical to {@link NormalFluidVolume}, but without an amount and with extra data hidden from public view. As such
 * this is safe to use in normal maps and sets. */
public abstract class NormalFluidKey extends FluidKey {

    @Nonnull
    public final Fluid fluid;

    public NormalFluidKey(Fluid fluid) {
        this(fluid, MISSING_SPRITE_IDENTIFIER, 0xFF_FF_FF);
    }

    public NormalFluidKey(Fluid fluid, Identifier spriteId) {
        this(fluid, spriteId, 0xFF_FF_FF);
    }

    public NormalFluidKey(Fluid fluid, Identifier spriteId, int renderColor) {
        super(new FluidRegistryEntry<>(Registry.FLUID, fluid), spriteId, renderColor);
        if (fluid == null) {
            throw new NullPointerException("fluid");
        }
        if (fluid instanceof EmptyFluid && fluid != Fluids.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (fluid instanceof BaseFluid && fluid != ((BaseFluid) fluid).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
        this.fluid = fluid;
    }

    @Override
    public abstract NormalFluidVolume readVolume(CompoundTag tag);

    @Override
    public abstract NormalFluidVolume withAmount(int amount);

    /** A {@link NormalFluidKey} with no extra data. */
    public static final class SimpleFluidKey extends NormalFluidKey {

        public SimpleFluidKey(Fluid fluid) {
            super(fluid);
        }

        public SimpleFluidKey(Fluid fluid, Identifier spriteId) {
            super(fluid, spriteId);
        }

        public SimpleFluidKey(Fluid fluid, Identifier spriteId, int renderColor) {
            super(fluid, spriteId, renderColor);
        }

        @Override
        public NormalFluidVolume readVolume(CompoundTag tag) {
            return new SimpleFluidVolume(this, tag);
        }

        @Override
        public NormalFluidVolume withAmount(int amount) {
            return new SimpleFluidVolume(this, amount);
        }
    }
}
