package alexiil.mc.lib.attributes.fluid;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/** Identical to {@link FluidVolume}, but without an amount and with extra data hidden from public view. As such this is
 * safe to use in normal maps and sets. */
public final class FluidKey {

    // Public because the tag is kept private, so we know it never changes.
    // (and it's a final field set to null so there's that)
    public static final FluidKey EMPTY = new FluidKey(Fluids.EMPTY);

    private static final String KEY_IDENTIFIER = "Fluid";
    private static final String KEY_DATA = "ExtraData";

    @Nonnull
    public final Fluid fluid;

    @Nullable
    private final CompoundTag extraData;

    // Precomputed because the whole point is to use them in maps and sets.
    private final int hash;

    public FluidKey(Fluid fluid) {
        this(fluid, null);
    }

    public FluidKey(Fluid fluid, CompoundTag extraData) {
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
        this.extraData = copyOf(extraData);
        this.hash = isEmpty() ? 0 : Objects.hash(Registry.FLUID.getId(this.fluid), this.extraData);
    }

    public static FluidKey fromTag(CompoundTag tag) {
        if (tag.isEmpty()) {
            return FluidKey.EMPTY;
        }
        String idStr = tag.getString(KEY_IDENTIFIER);
        CompoundTag data = tag.getCompound(KEY_DATA);

        Identifier id = Identifier.create(idStr);
        // Identifier.create returns null if the string passed in is invalid
        if (id == null) {
            // Keep the extra data - just in case someone wants to use it.
            return new FluidKey(Fluids.EMPTY, data);
        }
        return new FluidKey(Registry.FLUID.get(id), data);
    }

    private static CompoundTag copyOf(CompoundTag tag) {
        return tag != null ? tag.method_10553() : null;
    }

    public CompoundTag toTag() {
        return toTag(new CompoundTag());
    }

    public CompoundTag toTag(CompoundTag tag) {
        if (isEmpty()) {
            return tag;
        }
        tag.putString(KEY_IDENTIFIER, Registry.FLUID.getId(fluid).toString());
        if (this.extraData != null) {
            tag.put(KEY_DATA, copyOf(extraData));
        }
        return tag;
    }

    @Nullable
    public CompoundTag getExtraData() {
        return copyOf(extraData);
    }

    @Nullable
    public CompoundTag getExtraData(String subKey) {
        if (extraData != null) {
            return copyOf(extraData.getCompound(subKey));
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        FluidKey other = (FluidKey) obj;
        return fluid == other.fluid && Objects.equals(extraData, other.extraData);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public boolean isEmpty() {
        return fluid == Fluids.EMPTY;
    }

    public FluidVolume withAmount(int amount) {
        return new FluidVolume(fluid, amount, copyOf(extraData));
    }
}
