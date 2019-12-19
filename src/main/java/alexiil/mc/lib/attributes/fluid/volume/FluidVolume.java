/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.potion.Potion;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount.FluidMergeResult;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount.FluidMergeRounding;
import alexiil.mc.lib.attributes.fluid.render.DefaultFluidVolumeRenderer;
import alexiil.mc.lib.attributes.fluid.render.FluidRenderFace;
import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer;

public abstract class FluidVolume {

    /** The base unit for all fluids. This is arbitrarily chosen to be 1 / 1620 of a bucket. NOTE: You should
     * <i>never</i> tell the player what unit this is!
     * 
     * @deprecated Fluids now use {@link FluidAmount fractions} instead of a single base unit - which makes this
     *             completely deprecated with no replacement. */
    // and to establish easy compatibility with silk, which is where the numbers came from
    @Deprecated
    public static final int BASE_UNIT = 1;

    /** @deprecated Replaced by {@link FluidAmount#BUCKET} */
    @Deprecated
    public static final int BUCKET = 20 * 9 * 9 * BASE_UNIT;

    /** @deprecated Replaced by {@link FluidAmount#BOTTLE} */
    @Deprecated
    public static final int BOTTLE = BUCKET / 3;

    static final String KEY_AMOUNT_1620INT = "Amount";
    static final String KEY_AMOUNT_LBA_FRACTION = "AmountF";

    public final FluidKey fluidKey;

    private FluidAmount amount;

    /** @param amount The amount, in (amount / 1620) */
    @Deprecated
    public FluidVolume(FluidKey key, int amount) {
        this(key, FluidAmount.of1620(amount));
    }

    public FluidVolume(FluidKey key, FluidAmount amount) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        Fluid rawFluid = key.getRawFluid();
        if (rawFluid instanceof EmptyFluid && key != FluidKeys.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (rawFluid instanceof BaseFluid && rawFluid != ((BaseFluid) rawFluid).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
        this.fluidKey = key;
        this.amount = amount;

        if (key.entry.isEmpty()) {
            if (!amount.isZero()) {
                throw new IllegalArgumentException("Empty Fluid Volume's must have an amount of 0!");
            }
        } else if (amount.isNegative()) {
            throw new IllegalArgumentException("Fluid Volume's must have an amount greater than 0!");
        }
    }

    public FluidVolume(FluidKey key, CompoundTag tag) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        Fluid rawFluid = key.getRawFluid();
        if (rawFluid instanceof EmptyFluid && key != FluidKeys.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (rawFluid instanceof BaseFluid && rawFluid != ((BaseFluid) rawFluid).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }

        this.fluidKey = key;

        if (key.entry.isEmpty()) {
            this.amount = FluidAmount.ZERO;
        } else if (tag.contains(KEY_AMOUNT_1620INT)) {
            int readAmount = tag.getInt(KEY_AMOUNT_1620INT);
            this.amount = FluidAmount.of1620(Math.max(1, readAmount));
        } else {
            this.amount = FluidAmount.fromNbt(tag.getCompound(KEY_AMOUNT_LBA_FRACTION));
            if (amount.isNegative()) {
                amount = amount.negate();
            }
        }
    }

    public static FluidVolume fromTag(CompoundTag tag) {
        if (tag.isEmpty()) {
            return FluidKeys.EMPTY.withAmount(FluidAmount.ZERO);
        }
        return FluidKey.fromTag(tag).readVolume(tag);
    }

    public final CompoundTag toTag() {
        return toTag(new CompoundTag());
    }

    public CompoundTag toTag(CompoundTag tag) {
        if (isEmpty()) {
            return tag;
        }
        fluidKey.toTag(tag);
        tag.put(KEY_AMOUNT_LBA_FRACTION, amount.toNbt());
        return tag;
    }

    /** Creates a new {@link FluidVolume} from the given fluid, with the given amount stored. This just delegates
     * internally to {@link FluidKey#withAmount(int)}. */
    @Deprecated
    public static FluidVolume create(FluidKey fluid, int amount) {
        return fluid.withAmount(amount);
    }

    /** Creates a new {@link FluidVolume} from the given fluid, with the given amount stored. */
    @Deprecated
    public static FluidVolume create(Fluid fluid, int amount) {
        return FluidKeys.get(fluid).withAmount(amount);
    }

    /** Creates a new {@link FluidVolume} from the given potion, with the given amount stored. */
    @Deprecated
    public static FluidVolume create(Potion potion, int amount) {
        return FluidKeys.get(potion).withAmount(amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        FluidVolume other = (FluidVolume) obj;
        if (isEmpty()) {
            return other.isEmpty();
        }
        if (other.isEmpty()) {
            return false;
        }
        return amount == other.amount//
            && Objects.equals(fluidKey, other.fluidKey);
    }

    @Override
    public int hashCode() {
        if (isEmpty()) {
            return 0;
        } else {
            return fluidKey.hashCode() + 31 * amount.hashCode();
        }
    }

    @Override
    public String toString() {
        return fluidKey + " " + localizeAmount();
    }

    public String localizeAmount() {
        return fluidKey.unitSet.localizeAmount(getAmount_F());
    }

    /** @deprecated Use {@link Objects#equals(Object)} instead of this. */
    @Deprecated
    public static boolean areFullyEqual(FluidVolume a, FluidVolume b) {
        return Objects.equals(a, b);
    }

    public static boolean areEqualExceptAmounts(FluidVolume a, FluidVolume b) {
        if (a.isEmpty()) {
            return b.isEmpty();
        } else if (b.isEmpty()) {
            return false;
        }
        return a.getFluidKey().equals(b.getFluidKey());
    }

    public final boolean isEmpty() {
        return fluidKey == FluidKeys.EMPTY || amount.isZero();
    }

    public FluidKey getFluidKey() {
        return fluidKey;
    }

    /** @return The minecraft {@link Fluid} instance that this contains, or null if this is based on something else
     *         (like {@link Potion}'s). */
    @Nullable
    public Fluid getRawFluid() {
        return getFluidKey().getRawFluid();
    }

    public final FluidVolume copy() {
        return isEmpty() ? FluidKeys.EMPTY.withAmount(FluidAmount.ZERO) : copy0();
    }

    protected FluidVolume copy0() {
        return getFluidKey().withAmount(amount);
    }

    /** @deprecated Replaced by {@link #getAmount_F()} instead. */
    @Deprecated
    public final int getAmount() {
        return isEmpty() ? 0 : getRawAmount();
    }

    public final FluidAmount getAmount_F() {
        return isEmpty() ? FluidAmount.ZERO : getRawAmount_F();
    }

    /** @return The raw amount value, which might not be 0 if this is {@link #isEmpty() empty}. */
    @Deprecated
    protected final int getRawAmount() {
        return amount.as1620();
    }

    protected final FluidAmount getRawAmount_F() {
        return amount;
    }

    /** Protected to allow the implementation of {@link #split(int)} and
     * {@link #merge0(FluidVolume, FluidMergeRounding)} to set the amount. */
    @Deprecated
    protected final void setAmount(int newAmount) {
        setAmount(FluidAmount.of1620(newAmount));
    }

    /** Protected to allow the implementation of {@link #split(FluidAmount)} and
     * {@link #merge0(FluidVolume, FluidMergeRounding)} to set the amount. */
    protected final void setAmount(FluidAmount newAmount) {
        // Note that you can always set the amount to 0 to make this volume empty
        if (newAmount.isNegative()) {
            throw new IllegalArgumentException("newAmount was less than 0! (was " + newAmount + ")");
        }
        this.amount = newAmount;
    }

    /** Merges as much fluid as possible from the source into the target, leaving the leftover in the
     * 
     * @param source The source fluid. This <em>will</em> be modified if any is moved.
     * @param target The destination fluid. This <em>will</em> be modified if any is moved.
     * @return True if the merge was successful, false otherwise. If either fluid is empty or if they have different
     *         {@link #getFluidKey() keys} then this will return false (and fail). */
    public static boolean mergeInto(FluidVolume source, FluidVolume target) {
        return mergeInto(source, target, FluidMergeRounding.DEFAULT, Simulation.SIMULATE);
    }

    /** Merges as much fluid as possible from the source into the target, leaving the leftover in the
     * 
     * @param source The source fluid. This <em>will</em> be modified if any is moved.
     * @param target The destination fluid. This <em>will</em> be modified if any is moved.
     * @param rounding
     * @return True if the merge was successful, false otherwise. If either fluid is empty or if they have different
     *         {@link #getFluidKey() keys} then this will return false (and fail). */
    public static boolean mergeInto(FluidVolume source, FluidVolume target, FluidMergeRounding rounding) {
        return mergeInto(source, target, rounding, Simulation.SIMULATE);
    }

    /** Merges as much fluid as possible from the source into the target, leaving the leftover in the source.
     * 
     * @param source The source fluid. This <em>will</em> be modified if any is moved.
     * @param target The destination fluid. This <em>will</em> be modified if any is moved.
     * @param rounding
     * @return True if the merge was successful, false otherwise. If either fluid is empty or if they have different
     *         {@link #getFluidKey() keys} then this will return false (and fail). */
    public static boolean mergeInto(
        FluidVolume source, FluidVolume target, FluidMergeRounding rounding, Simulation simulation
    ) {
        if (source.isEmpty() || target.isEmpty()) {
            return false;
        }
        if (source.getFluidKey() != target.getFluidKey()) {
            return false;
        }
        return source.merge(target, rounding, simulation);
    }

    /** @param a The merge target. Might be modified and/or returned.
     * @param b The other fluid. Might be modified, and might be returned.
     * @return the merged fluid. Might be either a or b depending on */
    @Nullable
    public static FluidVolume merge(FluidVolume a, FluidVolume b) {
        return merge(a, b, FluidMergeRounding.DEFAULT);
    }

    /** @param a The merge target. Might be modified and/or returned.
     * @param b The other fluid. Might be modified, and might be returned.
     * @return the merged fluid. Might be either a or b depending on */
    @Nullable
    public static FluidVolume merge(FluidVolume a, FluidVolume b, FluidMergeRounding rounding) {
        if (a.isEmpty()) {
            if (b.isEmpty()) {
                return FluidKeys.EMPTY.withAmount(FluidAmount.ZERO);
            }
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        if (a.merge(b, rounding, Simulation.ACTION)) {
            return a;
        }
        return null;
    }

    /** Checks to see if the given {@link FluidVolume} can merge into this one. Returns false if either this fluid or
     * the given fluid are {@link #isEmpty() empty}. */
    public final boolean canMerge(FluidVolume with) {
        if (isEmpty() || with.isEmpty()) {
            return false;
        }
        return merge(with, Simulation.SIMULATE);
    }

    public final boolean merge(FluidVolume other, Simulation simulation) {
        return merge(other, FluidMergeRounding.ROUND_HALF_EVEN, simulation);
    }

    public final boolean merge(FluidVolume other, FluidMergeRounding rounding, Simulation simulation) {
        if (isEmpty() || other.isEmpty()) {
            throw new IllegalArgumentException("Don't try to merge two empty fluids!");
        }
        if (getClass() != other.getClass() || !Objects.equals(fluidKey, other.fluidKey)) {
            return false;
        }
        if (simulation == Simulation.ACTION) {
            merge0(other, rounding);
        }
        return true;
    }

    /** Actually merges two {@link FluidVolume}'s together.
     * 
     * @param other The other fluid volume. This will always be the same class as this. This should change the amount of
     *            the other fluid to 0. */
    protected void merge0(FluidVolume other, FluidMergeRounding rounding) {
        FluidMergeResult result = FluidAmount.merge(getAmount_F(), other.getAmount_F(), rounding);
        setAmount(result.merged);
        other.setAmount(result.excess);
    }

    /** @deprecated Replaced by {@link #split(FluidAmount)} */
    @Deprecated
    public final FluidVolume split(int toRemove) {
        return split(FluidAmount.of1620(toRemove));
    }

    /** Splits off the given amount of fluid and returns it, reducing this amount as well.<br>
     * If the given amount is greater than this then the returned {@link FluidVolume} will have an amount equal to this
     * amount, and not the amount given.
     * 
     * @param toRemove If zero then the empty fluid is returned.
     * @throws IllegalArgumentException if the given amount is negative. */
    public final FluidVolume split(FluidAmount toRemove) {
        return split(toRemove, RoundingMode.HALF_EVEN);
    }

    /** Splits off the given amount of fluid and returns it, reducing this amount as well.<br>
     * If the given amount is greater than this then the returned {@link FluidVolume} will have an amount equal to this
     * amount, and not the amount given.
     * 
     * @param toRemove If zero then the empty fluid is returned.
     * @throws IllegalArgumentException if the given amount is negative. */
    public final FluidVolume split(FluidAmount toRemove, RoundingMode rounding) {
        if (toRemove.isNegative()) {
            throw new IllegalArgumentException("Cannot split off a negative amount!");
        }
        if (toRemove.isZero() || isEmpty()) {
            return FluidVolumeUtil.EMPTY;
        }
        if (toRemove.isGreaterThan(amount)) {
            toRemove = amount;
        }
        return split0(toRemove, rounding);
    }

    /** @param toTake A valid subtractable amount.
     * @return A new {@link FluidVolume} with the given amount that has been removed from this. */
    protected FluidVolume split0(FluidAmount toTake, RoundingMode rounding) {
        setAmount(getAmount_F().roundedSub(toTake, rounding));
        return getFluidKey().withAmount(toTake);
    }

    /** Fallback for {@link DefaultFluidVolumeRenderer} to use if it can't find one itself.
     * 
     * @return An {@link Identifier} for the still sprite that this fluid volume should render with in gui's and
     *         in-world. */
    public Identifier getSprite() {
        return getFluidKey().spriteId;
    }

    /** Fallback for {@link DefaultFluidVolumeRenderer} to use if it can't find one itself.
     * <p>
     * Provided for completeness with {@link #getFlowingSprite()}. As this is final (and so cannot be overridden) it is
     * always safe to call this instead of {@link #getSprite()}. (If getSprite() is ever deprecated it is recommended to
     * that you call this instead).
     * 
     * @return An {@link Identifier} for the still sprite that this fluid volume should render with in gui's and
     *         in-world. */
    public final Identifier getStillSprite() {
        return getSprite();
    }

    /** Fallback for {@link DefaultFluidVolumeRenderer} to use if it can't find one itself.
     * 
     * @return An {@link Identifier} for the flowing sprite that this fluid volume should render with in gui's and
     *         in-world when {@link FluidRenderFace#flowing} is true. */
    public Identifier getFlowingSprite() {
        return getFluidKey().flowingSpriteId;
    }

    /** @return The colour tint to use when rendering this fluid volume in gui's or in-world. Note that this MUST be in
     *         0xRR_GG_BB format: <code>(r << 16) | (g << 8) | (b)</code> */
    public int getRenderColor() {
        return getFluidKey().renderColor;
    }

    public Text getName() {
        return getFluidKey().name;
    }

    @Environment(EnvType.CLIENT)
    public List<Text> getTooltipText(TooltipContext ctx) {
        List<Text> list = new ArrayList<>();
        list.add(getName());
        if (ctx.isAdvanced()) {
            FluidEntry entry = getFluidKey().entry;
            list.add(new LiteralText(entry.getRegistryInternalName()).formatted(Formatting.DARK_GRAY));
            list.add(new LiteralText(entry.getId().toString()).formatted(Formatting.DARK_GRAY));
        }
        return list;
    }

    /** Returns the {@link FluidVolumeRenderer} to use for rendering this fluid. */
    @Environment(EnvType.CLIENT)
    public FluidVolumeRenderer getRenderer() {
        return DefaultFluidVolumeRenderer.INSTANCE;
    }

    /** Delegate method to {@link #getRenderer()}.
     * {@link FluidVolumeRenderer#render(FluidVolume, List, VertexConsumerProvider, MatrixStack) render(faces, vcp,
     * matrices)}. */
    @Environment(EnvType.CLIENT)
    public final void render(List<FluidRenderFace> faces, VertexConsumerProvider vcp, MatrixStack matrices) {
        getRenderer().render(this, faces, vcp, matrices);
    }

    /** Delegate method to
     * {@link #getRenderer()}.{@link FluidVolumeRenderer#renderGuiRectangle(FluidVolume, double, double, double, double)} */
    @Environment(EnvType.CLIENT)
    public final void renderGuiRect(double x0, double y0, double x1, double y1) {
        getRenderer().renderGuiRectangle(this, x0, y0, x1, y1);
    }
}
