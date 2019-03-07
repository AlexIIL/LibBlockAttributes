package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

public abstract class FluidKey {

    public static final Identifier MISSING_SPRITE_IDENTIFIER = new Identifier("minecraft", "missingno");

    /* package-private */ final FluidRegistryEntry<?> registryEntry;

    /* The sprite and render color is package private because callers aren't meant to rely on these - instead they
     * should use the appropriate methods in FluidVolume as it might depend on data in that class. */
    /* package-private */ final Identifier spriteId;
    /* package-private */ final int renderColor;

    public FluidKey(FluidRegistryEntry<?> registryEntry) {
        this(registryEntry, MISSING_SPRITE_IDENTIFIER, 0xFF_FF_FF);
    }

    public FluidKey(FluidRegistryEntry<?> registryEntry, Identifier spriteId) {
        this(registryEntry, spriteId, 0xFF_FF_FF);
    }

    public FluidKey(FluidRegistryEntry<?> registryEntry, Identifier spriteId, int renderColor) {
        this.registryEntry = registryEntry;
        this.spriteId = spriteId;
        this.renderColor = renderColor;
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

    public abstract FluidVolume readVolume(CompoundTag tag);

    public final boolean isEmpty() {
        return this == FluidKeys.EMPTY;
    }

    public abstract FluidVolume withAmount(int amount);
}
