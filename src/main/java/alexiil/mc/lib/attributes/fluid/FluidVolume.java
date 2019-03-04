package alexiil.mc.lib.attributes.fluid;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/** {@link ItemStack} equivalent for {@link Fluid fluids}. */
public final class FluidVolume {

    /** The base unit for all fluids. This is arbitrarily chosen to be one twentieth the value of a nugget. NOTE: YOu
     * should <i>never</i> tell the player what unit this is! Instead */
    // and to establish easy compatibility with silk, which is where the numbers came from
    public static final int BASE_UNIT = 1;

    public static final int NUGGET = 20 * BASE_UNIT;
    public static final int INGOT = 9 * NUGGET;
    public static final int BLOCK = 9 * INGOT;

    public static final int BUCKET = BLOCK;
    public static final int BOTTLE = BUCKET / 3;

    private static final String KEY_IDENTIFIER = "Fluid";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_DATA = "ExtraData";

    @Nonnull
    public final Fluid fluid;

    /** The number of {@link #BASE_UNIT units} in this volume. */
    // Private because then we disallow fluids with negative amounts
    private int amount;

    @Nullable
    public CompoundTag extraData;

    /** Creates a new empty fluid. */
    public FluidVolume() {
        this(Fluids.EMPTY, 0);
    }

    public FluidVolume(Fluid fluid, int amount) {
        this(fluid, amount, null);
    }

    public FluidVolume(Fluid fluid, int amount, CompoundTag tag) {
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
        this.amount = amount;
        this.extraData = copyOf(tag);
    }

    public static FluidVolume fromTag(CompoundTag tag) {
        if (tag.isEmpty()) {
            return new FluidVolume();
        }
        String idStr = tag.getString(KEY_IDENTIFIER);
        int amount = tag.getInt(KEY_AMOUNT);
        if (amount < 0) {
            amount = 0;
        }
        CompoundTag data = tag.getCompound(KEY_DATA);

        Identifier id = Identifier.create(idStr);
        // Identifier.create returns null if the string passed in is invalid
        if (id == null) {
            // Keep the extra data - just in case someone wants to use it.
            return new FluidVolume(Fluids.EMPTY, amount, data);
        }
        Fluid fluid = Registry.FLUID.get(id);
        return new FluidVolume(fluid, amount, data);
    }

    private static CompoundTag copyOf(CompoundTag tag) {
        return tag != null ? tag.method_10553() : null;
    }

    public static boolean areFullyEqual(FluidVolume a, FluidVolume b) {
        boolean aIsEmpty = a.isEmpty();
        boolean bIsEmpty = b.isEmpty();
        if (aIsEmpty || bIsEmpty) {
            return aIsEmpty == bIsEmpty;
        }
        if (a.fluid != b.fluid) {
            return false;
        }
        if (a.amount != b.amount) {
            return false;
        }
        return Objects.equals(a.extraData, b.extraData);
    }

    public static boolean areEqualExceptAmounts(FluidVolume a, FluidVolume b) {
        boolean aIsEmpty = a.isEmpty();
        boolean bIsEmpty = b.isEmpty();
        if (aIsEmpty || bIsEmpty) {
            return aIsEmpty == bIsEmpty;
        }
        if (a.fluid != b.fluid) {
            return false;
        }
        return Objects.equals(a.extraData, b.extraData);
    }

    public FluidKey toKey() {
        if (isEmpty()) {
            return FluidKey.EMPTY;
        }
        return new FluidKey(fluid, extraData);
    }

    public boolean isEmpty() {
        return fluid == Fluids.EMPTY || amount == 0;
    }

    public FluidVolume copy() {
        return new FluidVolume(fluid, amount, copyOf(extraData));
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int newAmount) {
        if (newAmount < 0) {
            throw new IllegalArgumentException("Amount cannot be less than 0!");
        }
        this.amount = newAmount;
    }

    public void add(int toAdd) {
        int newAmount = this.amount + toAdd;
        if (newAmount < 0) {
            if (toAdd < 0) {
                throw new IllegalArgumentException(
                    "Tried to remove too much fluid! (The new amount was negative)\ntoRemove = " + -toAdd
                        + ", oldAmount = " + this.amount + ", newAmount = " + newAmount + ")");
            } else {
                throw new IllegalArgumentException(
                    "Tried to add too much fluid! (The new amount was negative)\ntoAdd = " + toAdd + ", oldAmount = "
                        + this.amount + ", newAmount = " + newAmount + ")");
            }
        }
        this.amount = newAmount;
    }

    public void subtract(int toSubtract) {
        add(-toSubtract);
    }

    public FluidVolume split(int toTake) {
        toTake = Math.min(toTake, amount);
        this.amount -= toTake;
        return new FluidVolume(fluid, toTake, copyOf(extraData));
    }

    @Override
    public String toString() {
        return fluid + " " + FluidVolumeUtil.localizeFluidAmount(amount);
    }
}
