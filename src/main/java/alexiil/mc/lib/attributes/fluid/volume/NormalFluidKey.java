package alexiil.mc.lib.attributes.fluid.volume;

import javax.annotation.Nonnull;

import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.TextComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.fluid.volume.NormalFluidVolume.SimpleFluidVolume;

/** Identical to {@link NormalFluidVolume}, but without an amount and with extra data hidden from public view. As such
 * this is safe to use in normal maps and sets. */
public abstract class NormalFluidKey extends FluidKey {

    public static class NormalFluidKeyBuilder extends FluidKeyBuilder {

        public final Fluid fluid;

        public NormalFluidKeyBuilder(Fluid fluid, Identifier spriteId, TextComponent name) {
            super(new FluidRegistryEntry<>(Registry.FLUID, fluid), spriteId, name);
            this.fluid = fluid;
        }

        @Override
        public NormalFluidKeyBuilder setRenderColor(int renderColor) {
            return (NormalFluidKeyBuilder) super.setRenderColor(renderColor);
        }
    }

    @Nonnull
    public final Fluid fluid;

    public NormalFluidKey(NormalFluidKeyBuilder builder) {
        super(builder);
        Fluid fl = builder.fluid;
        if (fl == null) {
            throw new NullPointerException("fluid");
        }
        if (fl instanceof EmptyFluid && fl != Fluids.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (fl instanceof BaseFluid && fl != ((BaseFluid) fl).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
        this.fluid = fl;
    }

    @Override
    public abstract NormalFluidVolume readVolume(CompoundTag tag);

    @Override
    public abstract NormalFluidVolume withAmount(int amount);

    /** A {@link NormalFluidKey} with no extra data. */
    public static final class SimpleFluidKey extends NormalFluidKey {

        public SimpleFluidKey(NormalFluidKeyBuilder builder) {
            super(builder);
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
