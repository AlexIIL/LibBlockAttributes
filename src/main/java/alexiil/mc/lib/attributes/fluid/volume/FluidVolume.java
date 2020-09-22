/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.io.IOException;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
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

/** An amount of a {@link FluidKey}, analogous to forge's FluidStack class or RebornCore's FluidInstance class. However
 * there are a few key differences:
 * <ol>
 * <li>FluidVolume is abstract, and it's subclasses must be defined by the {@link FluidKey} rather than anyone else. As
 * such you should always use the factory methods in {@link FluidKey}.</li>
 * <li>LBA doesn't have any direct way to store arbitrary data in a {@link CompoundTag}/NBT, so instead all custom data
 * must be stored in a way that's defined by the {@link FluidKey}, or a FluidProperty that's already been registered
 * with the {@link FluidKey}.</li>
 * <li>The amount field cannot be modified directly - instead you should either split or merge with any of the public
 * split or merge/mergeInto methods in this class. That way the custom data can handle splitting and merging properly.
 * Note that the only requirement for merging two {@link FluidVolume}s is that they have identical keys. (And are of the
 * same class, but that's implied by having the same key).</li>
 * </ol>
 */
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

    public static final JsonDeserializer<FluidVolume> DESERIALIZER = (json, type, ctx) -> {
        if (json.isJsonNull()) {
            return FluidVolumeUtil.EMPTY;
        }
        if (!json.isJsonObject()) {
            throw new JsonSyntaxException("Expected " + json + " to be an object!");
        }
        return FluidVolume.fromJson(json.getAsJsonObject());
    };

    public final FluidKey fluidKey;

    private FluidAmount amount;

    /** Property value array. If this is null (or any of it's entries are null) then it indicates that the property is
     * using it's default value, and hasn't explicitly been changed yet. Property keys are indicated in
     * {@link FluidKey#properties}. */
    private Object[] propertyValues;

    /** Internal constructor that validates the fluid key, leaving the amount to a different constructor. */
    private FluidVolume(FluidKey key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        Fluid rawFluid = key.getRawFluid();
        if (rawFluid instanceof EmptyFluid && key != FluidKeys.EMPTY) {
            throw new IllegalArgumentException("Different empty fluid!");
        }
        if (rawFluid instanceof FlowableFluid && rawFluid != ((FlowableFluid) rawFluid).getStill()) {
            throw new IllegalArgumentException("Only the still version of fluids are allowed!");
        }
        this.fluidKey = key;
        // Leave amount alone as it is required that every other constructor set it
    }

    /** @param amount The amount, in (amount / 1620) */
    @Deprecated
    public FluidVolume(FluidKey key, int amount) {
        this(key, FluidAmount.of1620(amount));
    }

    public FluidVolume(FluidKey key, FluidAmount amount) {
        this(key);
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
        this(key);
        if (key.entry.isEmpty()) {
            this.amount = FluidAmount.ZERO;
        } else {
            if (tag.contains(KEY_AMOUNT_1620INT)) {
                int readAmount = tag.getInt(KEY_AMOUNT_1620INT);
                this.amount = FluidAmount.of1620(Math.max(1, readAmount));
            } else {
                this.amount = FluidAmount.fromNbt(tag.getCompound(KEY_AMOUNT_LBA_FRACTION));
                if (amount.isNegative()) {
                    amount = amount.negate();
                }
            }
            if (!key.properties.isEmpty()) {
                CompoundTag properties = tag.getCompound("Properties");
                // End to Start so we only allocate the values array once.
                for (int index = key.properties.size() - 1; index >= 0; index--) {
                    FluidProperty<?> property = key.properties.get(index);
                    Tag propTag = properties.get(property.nbtKey);
                    if (propTag == null) {
                        continue;
                    }
                    Object value = property.fromTag(propTag);
                    if (value == null || value == property.defaultValue) {
                        continue;
                    }
                    putPropertyValue(property, value);
                }
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
        if (propertyValues != null) {
            CompoundTag properties = new CompoundTag();

            for (int index = propertyValues.length - 1; index >= 0; index--) {
                Object value = propertyValues[index];
                if (value == null) {
                    continue;
                }
                FluidProperty<?> property = fluidKey.properties.get(index);
                Tag propTag = propToTag(property, value);
                if (propTag != null) {
                    properties.put(property.nbtKey, propTag);
                }
            }

            if (!properties.isEmpty()) {
                tag.put("Properties", properties);
            }
        }
        return tag;
    }

    private static <T> Tag propToTag(FluidProperty<T> property, Object value) {
        return property.toTag(property.type.cast(value));
    }

    public FluidVolume(FluidKey key, JsonObject json) throws JsonSyntaxException {
        this(key);

        if (key.entry.isEmpty()) {
            amount = FluidAmount.ZERO;
        } else {
            JsonElement ja = json.get("amount");
            if (ja == null) {
                this.amount = FluidAmount.ZERO;
            } else {
                this.amount = parseAmount(ja);
            }
        }
    }

    public static FluidAmount parseAmount(JsonElement elem) throws JsonSyntaxException {
        if (elem == null) {
            throw new JsonSyntaxException("Expected 'amount' to be a string, but was missing!");
        } else {
            if (!elem.isJsonPrimitive()) {
                throw new JsonSyntaxException("Expected 'amount' to be a string, but was " + elem);
            }
            try {
                FluidAmount a = FluidAmount.parse(elem.getAsString());
                if (a.isNegative()) {
                    throw new JsonSyntaxException("Expected 'amount' to be either positive or zero, but was negative!");
                }
                return a;
            } catch (NumberFormatException nfe) {
                throw new JsonSyntaxException("Expected 'amount' to be a valid fluid amount!", nfe);
            }
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        fluidKey.toJson(json);
        if (!isEmpty()) {
            json.addProperty("amount", amount.toParseableString());
        }
        return json;
    }

    public static FluidVolume fromJson(JsonObject json) throws JsonSyntaxException {
        try {
            return FluidKey.fromJsonInternal(json).readVolume(json);
        } catch (JsonSyntaxException jse) {
            throw new JsonSyntaxException("Not a valid fluid volume: " + json, jse);
        }
    }

    public final void toMcBuffer(PacketByteBuf buffer) {
        if (isEmpty()) {
            FluidKeys.EMPTY.toMcBuffer(buffer);
        } else {
            fluidKey.toMcBuffer(buffer);
            toMcBufferInternal(buffer);
            writeProperties(buffer);
        }
    }

    protected void toMcBufferInternal(PacketByteBuf buffer) {
        amount.toMcBuffer(buffer);
    }

    public static FluidVolume fromMcBuffer(PacketByteBuf buffer) throws IOException {
        return FluidKey.fromMcBuffer(buffer).readVolume(buffer);
    }

    protected void fromMcBufferInternal(PacketByteBuf buffer) {
        // For sub-classes
    }

    final void writeProperties(PacketByteBuf buffer) {
        if (!fluidKey.properties.isEmpty()) {
            int countPresent = 0;
            for (Object obj : propertyValues) {
                if (obj != null) {
                    countPresent++;
                }
            }
            buffer.writeByte(countPresent);
            for (int i = propertyValues.length - 1; i >= 0; i--) {
                FluidProperty<?> prop = fluidKey.properties.get(i);
                Object val = propertyValues[i];
                if (val != null) {
                    // Really? Strings?
                    // (Unfortunately you'd *need* to use LNS to make this work
                    // properly with potentially incompatible modsets)
                    buffer.writeString(prop.nbtKey);
                    writeProp(prop, val, buffer);
                }
            }
        }
    }

    private static <T> void writeProp(FluidProperty<T> prop, Object value, PacketByteBuf buffer) {
        prop.writeToBuffer(buffer, prop.type.cast(value));
    }

    final void readProperties(PacketByteBuf buffer) {
        if (!fluidKey.properties.isEmpty()) {
            int count = buffer.readUnsignedByte();
            for (int i = 0; i < count; i++) {
                String key = buffer.readString();
                int propIndex = fluidKey.propertyKeys.getInt(key);
                if (propIndex < 0) {
                    throw new IllegalArgumentException("Unknown remote fluid property " + key + " for " + fluidKey);
                }
                readProp(buffer, fluidKey.properties.get(propIndex));
            }
        }
    }

    private <T> void readProp(PacketByteBuf buffer, FluidProperty<T> prop) {
        prop.set(this, prop.readFromBuffer(buffer));
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
        if (obj == null) {
            return false;
        }
        if (isEmpty()) {
            if (obj instanceof FluidVolume) {
                return ((FluidVolume) obj).isEmpty();
            }
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        FluidVolume other = (FluidVolume) obj;
        if (other.isEmpty()) {
            return false;
        }
        return amount.equals(other.amount)//
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

    public final String localizeAmount(FluidTooltipContext ctx) {
        return fluidKey.unitSet.localizeAmount(getAmount_F(), getName(), ctx);
    }

    public String localizeInTank(FluidAmount capacity) {
        return fluidKey.unitSet.localizeTank(getAmount_F(), capacity);
    }

    public final String localizeInTank(FluidAmount capacity, FluidTooltipContext ctx) {
        return fluidKey.unitSet.localizeTank(getAmount_F(), capacity, getName(), ctx);
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

    /** @return The {@link FluidKey} for this volume. Subclasses may override this to use a return type for their key
     *         class. */
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
        if (isEmpty()) {
            return FluidKeys.EMPTY.withAmount(FluidAmount.ZERO);
        }
        FluidVolume copy = copy0();
        copyPropertiesInto(copy);
        return copy;
    }

    protected FluidVolume copy0() {
        return getFluidKey().withAmount(amount);
    }

    private final void copyPropertiesInto(FluidVolume dest) {
        dest.propertyValues = propertyValues == null ? null : Arrays.copyOf(propertyValues, propertyValues.length);
    }

    /** @deprecated Replaced by {@link #getAmount_F()} and {@link #amount()}. */
    @Deprecated
    public final int getAmount() {
        return isEmpty() ? 0 : getRawAmount();
    }

    /** Note: due to LBA's backwards compatibility with when it used to use a 1620-based fixed fraction integers this
     * cannot use the name "getAmount", so instead this has "_F" added to the end. Alternatively you can use
     * {@link #amount()} if you prefer a more reasonable name.
     * 
     * @return {@link FluidAmount#ZERO} if this {@link #isEmpty()}, otherwise this returns the fractional amount
     *         stored. */
    public final FluidAmount getAmount_F() {
        return amount();
    }

    /** An alternate name for {@link #getAmount_F()}.
     * 
     * @return {@link FluidAmount#ZERO} if this {@link #isEmpty()}, otherwise this returns the fractional amount
     *         stored. */
    public FluidAmount amount() {
        return isEmpty() ? FluidAmount.ZERO : getRawAmount_F();
    }

    /** @return The raw amount value, which might not be 0 if this is {@link #isEmpty() empty}.
     * @deprecated Replaced by {@link #getRawAmount_F()}. */
    @Deprecated
    protected final int getRawAmount() {
        return amount.as1620();
    }

    /** @return The fractional amount of fluid that this holds. This might not be {@link FluidAmount#isZero()} if this
     *         {@link #isEmpty()}. */
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

    /** Creates a copy of this fluid with the given amount. Unlike calling {@link FluidKey#withAmount(FluidAmount)} this
     * will preserve any extra data that this {@link FluidVolume} contains. */
    public FluidVolume withAmount(FluidAmount newAmount) {
        FluidVolume fv = copy();
        fv.setAmount(newAmount);
        return fv;
    }

    /** Returns a new {@link FluidVolume} that is a copy of this one, but with an amount multiplied by the given amount.
     * 
     * @see #withAmount(FluidAmount) */
    public FluidVolume multiplyAmount(int by) {
        return withAmount(getAmount_F().mul(by));
    }

    /** Merges as much fluid as possible from the source into the target, leaving the result in the
     * 
     * @param source The source fluid. This <em>will</em> be modified if any is moved.
     * @param target The destination fluid. This <em>will</em> be modified if any is moved.
     * @return True if the merge was successful, false otherwise. If either fluid is empty or if they have different
     *         {@link #getFluidKey() keys} then this will return false (and fail). */
    public static boolean mergeInto(FluidVolume source, FluidVolume target) {
        return mergeInto(source, target, FluidMergeRounding.DEFAULT, Simulation.SIMULATE);
    }

    /** Merges as much fluid as possible from the source into the target, leaving the result in the
     * 
     * @param source The source fluid. This <em>will</em> be modified if any is moved.
     * @param target The destination fluid. This <em>will</em> be modified if any is moved.
     * @param rounding
     * @return True if the merge was successful, false otherwise. If either fluid is empty or if they have different
     *         {@link #getFluidKey() keys} then this will return false (and fail). */
    public static boolean mergeInto(FluidVolume source, FluidVolume target, FluidMergeRounding rounding) {
        return mergeInto(source, target, rounding, Simulation.SIMULATE);
    }

    /** Merges as much fluid as possible from the source into the target, leaving the result in the source.
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
     * @return the inTank fluid. Might be either a or b depending on */
    @Nullable
    public static FluidVolume merge(FluidVolume a, FluidVolume b) {
        return merge(a, b, FluidMergeRounding.DEFAULT);
    }

    /** @param a The merge target. Might be modified and/or returned.
     * @param b The other fluid. Might be modified, and might be returned.
     * @return the inTank fluid. Might be either a or b depending on */
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
            FluidMergeResult merged = FluidAmount.merge(getAmount_F(), other.getAmount_F(), rounding);

            Object[] result = null;

            if (propertyValues != null || other.propertyValues != null) {

                int count = fluidKey.properties.size();
                for (int index = 0; index < count; index++) {
                    FluidProperty<?> prop = fluidKey.properties.get(index);

                    Object value = propertyValues != null ? propertyValues[index] : null;
                    if (value == null) {
                        value = prop.defaultValue;
                    }
                    Object valueOther = other.propertyValues != null ? other.propertyValues[index] : null;
                    if (valueOther == null) {
                        valueOther = prop.defaultValue;
                    }

                    if (value != valueOther) {
                        value = mergeGenericBypass(other, merged, prop, value, valueOther);
                    }

                    if (value != prop.defaultValue) {
                        if (result == null) {
                            result = new Object[count];
                        }
                        result[index] = value;
                    }
                }
            }
            propertyValues = result;
            merge0(other, rounding);
        }
        return true;
    }

    private <T> T mergeGenericBypass(
        FluidVolume other, FluidMergeResult merged, FluidProperty<T> prop, Object value, Object valueOther
    ) {
        T valA = prop.type.cast(value);
        T valB = prop.type.cast(valueOther);
        return prop.merge(this, other, merged.merged, valA, valB);
    }

    /** Actually merges two {@link FluidVolume}'s together. Only
     * {@link #merge(FluidVolume, FluidMergeRounding, Simulation)} should call this. (Except for subclasses that
     * override this method).
     * 
     * @param other The other fluid volume. This will always be the same class as this. This should change the amount of
     *            the other fluid to 0.
     * @deprecated because {@link #mergeInternal(FluidVolume, FluidMergeResult)} allows every method to share the same
     *             {@link FluidMergeResult} object, which reduces the chance to make a mistake when merging the two
     *             amounts. In addition it's a bit wasteful to re-compute the same value more than once.
     *             <p>
     *             So instead of overriding this it's recommended that you <em>only</em> override
     *             {@link #mergeInternal(FluidVolume, FluidMergeResult)}. */
    @Deprecated
    protected void merge0(FluidVolume other, FluidMergeRounding rounding) {
        mergeInternal(other, FluidAmount.merge(getAmount_F(), other.getAmount_F(), rounding));
    }

    /** Actually merges two {@link FluidVolume}'s together. Only
     * {@link #merge(FluidVolume, FluidMergeRounding, Simulation)} should call this. (Except for subclasses that
     * override this method).
     * 
     * @param other The other fluid volume. This will always be the same class as this. This should change the amount of
     *            the other fluid to {@link FluidMergeResult#excess}. */
    protected void mergeInternal(FluidVolume other, FluidMergeResult mergedAmounts) {
        setAmount(mergedAmounts.merged);
        other.setAmount(mergedAmounts.excess);
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
        FluidVolume split = split0(toRemove, rounding);
        copyPropertiesInto(split);
        return split;
    }

    /** @param toTake A valid subtractable amount.
     * @return A new {@link FluidVolume} with the given amount that has been removed from this. */
    protected FluidVolume split0(FluidAmount toTake, RoundingMode rounding) {
        setAmount(getAmount_F().roundedSub(toTake, rounding));
        return getFluidKey().withAmount(toTake);
    }

    /** @throws IllegalArgumentException if the given property hasn't been registered to the {@link FluidKey}. */
    public final <T> T getProperty(FluidProperty<T> property) {
        int index = fluidKey.propertyKeys.getOrDefault(property, -1);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown/unregistered property " + property + " for key " + fluidKey);
        }
        if (propertyValues == null || propertyValues.length <= index) {
            return property.defaultValue;
        }
        Object value = propertyValues[index];
        if (value == null) {
            return property.defaultValue;
        }
        if (property.type.isInstance(value)) {
            return property.type.cast(value);
        } else {
            throw new IllegalStateException(
                "The value in the propertyValues array (" + value.getClass() + " wasn't an instance of the required "
                    + property.type + ")"
            );
        }
    }

    private final <T> void putPropertyValue(FluidProperty<T> property, Object value) {
        setProperty(property, property.type.cast(value));
    }

    public final <T> void setProperty(FluidProperty<T> property, T value) {
        int index = fluidKey.propertyKeys.getOrDefault(property, -1);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown/unregistered property " + property + " for key " + fluidKey);
        }

        // Don't do an explicit equals() because this is just an optimisation.
        if (value == null || value == property.defaultValue) {
            value = null;
            if (propertyValues == null) {
                return;
            } else if (propertyValues.length < index + 1) {
                return;
            }
        } else {
            if (propertyValues == null) {
                propertyValues = new Object[index + 1];
            } else if (propertyValues.length < index + 1) {
                propertyValues = Arrays.copyOf(propertyValues, index + 1);
            }
        }
        propertyValues[index] = value;
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
     *         0xAA_RR_GG_BB format: <code>(r << 16) | (g << 8) | (b)</code>. Alpha may be omitted however - which
     *         should default it to 0xFF. */
    public int getRenderColor() {
        return getFluidKey().renderColor;
    }

    public Text getName() {
        return getFluidKey().name;
    }

    // Tooltips

    /** @deprecated Replaced by {@link #getFullTooltip()}. */
    @Deprecated
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

    /** Simple getter for retrieving the entire fluid tooltip, instead of adding it to an already-existing list.
     * 
     * @see #addFullTooltip(List)
     * @see #addFullTooltip(FluidAmount, FluidTooltipContext, List) */
    public final List<Text> getFullTooltip() {
        return getFullTooltip(null, FluidTooltipContext.USE_CONFIG);
    }

    /** Adds the entire tooltip for this fluid (by itself, not in a tank) to the given list.
     * 
     * @see #addFullTooltip(FluidAmount, FluidTooltipContext, List) */
    public final void addFullTooltip(List<Text> tooltip) {
        addFullTooltip(null, FluidTooltipContext.USE_CONFIG, tooltip);
    }

    /** Simple getter for retrieving the entire fluid tooltip, instead of adding it to an already-existing list.
     * 
     * @param capacity If non-null then this will display as if this was in a tank, rather than by itself. */
    public final List<Text> getFullTooltip(@Nullable FluidAmount capacity) {
        return getFullTooltip(capacity, FluidTooltipContext.USE_CONFIG);
    }

    /** Adds the complete tooltip for this {@link FluidVolume} to the given tooltip.
     * 
     * @param capacity If non-null then this will display as if this was in a tank, rather than by itself. */
    public final void addFullTooltip(@Nullable FluidAmount capacity, List<Text> tooltip) {
        addFullTooltip(capacity, FluidTooltipContext.USE_CONFIG, tooltip);
    }

    /** Simple getter for retrieving the entire fluid tooltip, instead of adding it to an already-existing list.
     * 
     * @param capacity If non-null then this will display as if this was in a tank, rather than by itself. */
    public final List<Text> getFullTooltip(@Nullable FluidAmount capacity, FluidTooltipContext context) {
        List<Text> tooltip = new ArrayList<>();
        addFullTooltip(capacity, context, tooltip);
        return tooltip;
    }

    /** Adds the complete tooltip for this {@link FluidVolume} to the given tooltip.
     * 
     * @param capacity If non-null then this will display as if this was in a tank, rather than by itself. */
    public final void addFullTooltip(@Nullable FluidAmount capacity, FluidTooltipContext context, List<Text> tooltip) {
        addTooltipNameAmount(capacity, context, tooltip);
        addTooltipExtras(context, tooltip);
        addTooltipTemperature(context, tooltip);
        addTooltipProperties(context, tooltip);
    }

    /** Adds the name and amount to the given tooltip. This is only provided so that custom tooltip implementations
     * can */
    public final void addTooltipNameAmount(
        @Nullable FluidAmount capacity, FluidTooltipContext context, List<Text> tooltip
    ) {
        Text name = context.stripFluidColours(getName());
        if (context.shouldJoinNameWithAmount()) {
            if (capacity != null) {
                tooltip.add(fluidKey.unitSet.getTank(amount, capacity, name, context));
            } else {
                tooltip.add(fluidKey.unitSet.getAmount(amount, name, context));
            }
        } else {
            tooltip.add(name);
            if (capacity != null) {
                tooltip.add(fluidKey.unitSet.getTank(amount, capacity, context));
            } else {
                tooltip.add(fluidKey.unitSet.getAmount(amount, context));
            }
        }
    }

    /** Adds any additional data that this {@link FluidVolume} has. */
    public void addTooltipExtras(FluidTooltipContext context, List<Text> tooltip) {
        fluidKey.addTooltipExtras(context, tooltip);
    }

    public final void addTooltipTemperature(FluidTooltipContext context, List<Text> tooltip) {
        FluidTemperature temp = fluidKey.getTemperature();
        if (temp != null) {
            temp.addTemperatureToTooltip(this, context, tooltip);
        }
    }

    public final void addTooltipProperties(FluidTooltipContext context, List<Text> tooltip) {
        for (FluidProperty<?> prop : fluidKey.properties) {
            prop.addTooltipExtras(this, context, tooltip);
        }
    }

    // Rendering

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
