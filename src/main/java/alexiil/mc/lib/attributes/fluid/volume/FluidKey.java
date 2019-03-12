package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.TextComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ViewableWorld;

public abstract class FluidKey {

    /* package-private */ final FluidRegistryEntry<?> registryEntry;

    /** The sprite to use when rendering this {@link FluidKey}'s specifically.
     * <p>
     * Note that this might differ from the one returned by {@link FluidVolume#getSprite()}! */
    public final Identifier spriteId;

    /** The sprite to use when rendering this {@link FluidKey}'s specifically.
     * <p>
     * Note that this might differ from the one returned by {@link FluidVolume#getRenderColor()}! */
    public final int renderColor;

    /** The name to use when displaying tooltips for this {@link FluidKey} specifically.
     * <p>
     * Note that this might differ from the one returned by {@link FluidVolume#getName()}! */
    public final TextComponent name;

    public static class FluidKeyBuilder {
        private final FluidRegistryEntry<?> registryEntry;
        private final Identifier spriteId;
        private final TextComponent name;
        private int renderColor = 0xFF_FF_FF;

        public FluidKeyBuilder(FluidRegistryEntry<?> registryEntry, Identifier spriteId, TextComponent name) {
            this.registryEntry = registryEntry;
            this.spriteId = spriteId;
            this.name = name;
        }

        public FluidKeyBuilder setRenderColor(int renderColor) {
            this.renderColor = renderColor;
            return this;
        }
    }

    public FluidKey(FluidKeyBuilder builder) {
        if (builder.registryEntry == null) {
            throw new NullPointerException("registryEntry");
        }
        if (builder.spriteId == null) {
            throw new NullPointerException("spriteId");
        }
        if (builder.name == null) {
            throw new NullPointerException("textComponent");
        }
        this.registryEntry = builder.registryEntry;
        this.spriteId = builder.spriteId;
        this.name = builder.name;
        this.renderColor = builder.renderColor;
    }

    public static final int swapArgbForAbgr(int colour) {
        return ((colour & 0xFF) << 16) | (colour & 0xFF_00) | ((colour & 0xFF_00_00) >> 16);
    }

    public static FluidKey fromTag(CompoundTag tag) {
        if (tag.isEmpty()) {
            return FluidKeys.EMPTY;
        }
        FluidKey fluidKey = FluidKeys.get(FluidRegistryEntry.fromTag(tag));
        if (fluidKey == null) {
            return FluidKeys.EMPTY;
        }
        return fluidKey;
    }

    public final CompoundTag toTag() {
        return toTag(new CompoundTag());
    }

    public final CompoundTag toTag(CompoundTag tag) {
        if (isEmpty()) {
            return tag;
        }
        registryEntry.toTag(tag);
        return tag;
    }

    @Override
    public String toString() {
        return registryEntry.toString();
    }

    public abstract FluidVolume readVolume(CompoundTag tag);

    public final boolean isEmpty() {
        return this == FluidKeys.EMPTY;
    }

    public abstract FluidVolume withAmount(int amount);

    /** Called when this is pumped out from the world. */
    public FluidVolume fromWorld(ViewableWorld world, BlockPos pos) {
        return withAmount(FluidVolume.BLOCK);
    }
}
