package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.StringTextComponent;
import net.minecraft.text.TextComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ViewableWorld;

public abstract class FluidKey {

    public static final Identifier MISSING_SPRITE_IDENTIFIER = new Identifier("minecraft", "missingno");
    public static final TextComponent MISSING_NAME = new StringTextComponent("!MISSING_NAME!");

    /* package-private */ final FluidRegistryEntry<?> registryEntry;

    /* The sprite and render colour is package private because callers aren't meant to rely on these - instead they
     * should use the appropriate methods in FluidVolume as it might depend on data in that class. */
    /* package-private */ final Identifier spriteId;
    /* package-private */ final int renderColor;
    /* package-private */ final TextComponent textComponent;

    public static class FluidKeyBuilder {
        private final FluidRegistryEntry<?> registryEntry;
        private Identifier spriteId = MISSING_SPRITE_IDENTIFIER;
        private int renderColor = 0xFF_FF_FF;
        private TextComponent textComponent = MISSING_NAME;

        public FluidKeyBuilder(FluidRegistryEntry<?> registryEntry) {
            this.registryEntry = registryEntry;
        }

        public FluidKeyBuilder setSpriteId(Identifier spriteId) {
            this.spriteId = spriteId;
            return this;
        }

        public FluidKeyBuilder setRenderColor(int renderColor) {
            this.renderColor = renderColor;
            return this;
        }

        public FluidKeyBuilder setTextComponent(TextComponent textComponent) {
            this.textComponent = textComponent;
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
        if (builder.textComponent == null) {
            throw new NullPointerException("textComponent");
        }
        this.registryEntry = builder.registryEntry;
        this.spriteId = builder.spriteId;
        this.renderColor = builder.renderColor;
        this.textComponent = builder.textComponent;
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
