/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSyntaxException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.volume.FluidTemperature.ContinuousFluidTemperature;
import alexiil.mc.lib.attributes.fluid.volume.FluidTemperature.DiscreteFluidTemperature;

/** Some data that can be added to {@link FluidVolume}s.
 * <p>
 * They have the following requirements:
 * <ol>
 * <li>Property values should be immutable. This allows multiple {@link FluidVolume}s to share the same object, and to
 * allow {@link #defaultValue} to exist as a public final field. If you want to store a mutable value as a property in a
 * fluid volume then you should copy it before modifying it, as every client will expect it to be unchanged.</li>
 * <li>Property values should be independent to the amount of fluid in a volume. This allows {@link FluidVolume}s to
 * leave properties alone until they are changed from their default values.</li>
 * <li>Property values that are {@link Object#equals(Object) equal} (or are identical with ==) don't need to be
 * merged.</li>
 * <li>Property values don't need any special handling to split them - the value will be used by both the volumes.</li>
 * <li>All values that a property can take can be merged. (This is done to keep the singular requirement that fluid
 * volumes can always be merged if they have the same key).</li>
 * </ol>
 * Note that it's generally more efficient to subclass {@link FluidVolume} if you can rather than add properties to one,
 * especially if the value you are storing is a primitive.
 * <p>
 * All of the simplifications allow optimising the storing array to store null instead of a value, and possibly use null
 * for the entire array if every property is set to it's default value. */
public abstract class FluidProperty<T> {

    /** Used internally to sort properties into a stable order. This sorts on {@link #nbtKey}. */
    /* package-private */ static final Comparator<FluidProperty<?>> COMPARATOR = (a, b) -> a.nbtKey.compareTo(b.nbtKey);

    /** Used for reading and writing this property. */
    public final Identifier id;
    public final Class<T> type;
    public final T defaultValue;

    /** The temperature scale, if this fluid property provides one, or implements {@link ContinuousFluidTemperature}
     * directly. */
    @Nullable
    public final ContinuousFluidTemperature temperature;

    /** The key that's used for serialising with NBT and json. It's also used for {@link #COMPARATOR}. This is always
     * {@link #id}.{@link Identifier#toString()}, and is only present as an optimisation. */
    /* package-private */ final String nbtKey;

    public FluidProperty(Identifier id, Class<T> type, T defaultValue) {
        this(id, type, defaultValue, null);
    }

    /** @param temperature The temperature property to use for this property. Must be null if this implements
     *            {@link ContinuousFluidTemperature} directly. */
    public FluidProperty(Identifier id, Class<T> type, T defaultValue, ContinuousFluidTemperature temperature) {
        Objects.requireNonNull(id, "The identifier must not be null!");
        Objects.requireNonNull(type, "The type must not be null!");
        Objects.requireNonNull(defaultValue, "The default value shouldn't be null!");
        this.id = id;
        this.type = type;
        this.defaultValue = defaultValue;
        this.nbtKey = id.toString();
        if (this instanceof FluidTemperature) {
            if (temperature != null) {
                throw new IllegalArgumentException(
                    "The FluidProperty " + getClass()
                        + " shouldn't implement FluidTemperature *and* pass a FluidTemperature!"
                );
            }
            if (!(this instanceof ContinuousFluidTemperature)) {
                throw new IllegalStateException(
                    "The FluidProperty " + getClass() + " should implement ContinuousFluidTemperature!"
                );
            }
            if (this instanceof DiscreteFluidTemperature) {
                throw new IllegalStateException(
                    "The FluidProperty " + getClass() + " must not implement DiscreteFluidTemperature!"
                );
            }
            this.temperature = (ContinuousFluidTemperature) this;
        } else {
            this.temperature = temperature;
        }
    }

    // ########
    // Utils
    // ########

    /** Helper method to obtain this property from the given fluid.
     * 
     * @see FluidVolume#getProperty(FluidProperty)
     * @throws IllegalArgumentException if the given property hasn't been registered to the {@link FluidKey}. */
    public final T get(FluidVolume volume) {
        return volume.getProperty(this);
    }

    /** Helper method to set a value for this property to the given fluid.
     * 
     * @throws IllegalArgumentException if the given property hasn't been registered to the {@link FluidKey}. */
    public final void set(FluidVolume volume, T value) {
        volume.setProperty(this, value);
    }

    // ################
    // Serialisation
    // ################

    /** @return The tag that will be passed to {@link #fromTag(Tag)} to return the value. Returning null indicates that
     *         nothing should be written out, and as such the default value will be used when reading. */
    @Nullable
    protected abstract Tag toTag(T value);

    /** Reads the value from the given tag.
     * 
     * @param tag A tag, which will probably have been generated from {@link #toTag(Object)}, but can also come from the
     *            user (via /give or similar). This will never be null, as a null or missing tag will instead use
     *            {@link #defaultValue}. */
    protected abstract T fromTag(Tag tag);

    /** Writes the given value out to a packet buffer. By default this writes the {@link Tag} returned by
     * {@link #toTag(Object)} in a {@link CompoundTag} to the buffer, so it's recommenced that you override this to
     * write out the value using a more efficient method. */
    protected void writeToBuffer(PacketByteBuf buffer, T value) {
        Tag tag = toTag(value);
        if (tag == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            CompoundTag comp = new CompoundTag();
            comp.put("k", tag);
            buffer.writeCompoundTag(comp);
        }
    }

    /** Writes the given value out to a packet buffer. By default this reads a {@link CompoundTag} from the buffer, so
     * it's recommenced that you override this to read out the value using a more efficient method.
     * 
     * @return The read value, or null if the default value should be used instead. (Which will most likely be stored as
     *         null instead). */
    @Nullable
    protected T readFromBuffer(PacketByteBuf buffer) {
        if (buffer.readBoolean()) {
            CompoundTag comp = buffer.readCompoundTag();
            Tag tag = comp != null ? comp.get("k") : null;
            if (tag == null) {
                return null;
            }
            return fromTag(tag);
        } else {
            return null;
        }
    }

    /** Reads the value from a {@link JsonElement}. It is highly recommended that you throw a
     * {@link JsonSyntaxException} rather than other internal exceptions if the given element isn't valid, as this is
     * likely to appear in a recipe json file. */
    protected T fromJson(JsonElement json) throws JsonSyntaxException {
        return defaultValue;
    }

    /** Writes the given value to a {@link JsonElement}.
     * 
     * @return The JsonElement that {@link #fromJson(JsonElement)} can read. The returned element won't be included if
     *         this returns {@link JsonNull}. */
    protected JsonElement toJson(T value) {
        return JsonNull.INSTANCE;
    }

    // ################
    // Behaviour
    // ################

    /** Merges two values together, using the two {@link FluidVolume}s for context. The volumes are only given for
     * context - most fluid properties will have no use for them.
     * <p>
     * This must never modify either of the passed {@link FluidVolume}s!
     * 
     * @param volumeA One of the volumes that will be merged.
     * @param volumeB The other volume that will be merged.
     * @param amount The new (merged) amount. This might not be exactly equal to
     *            volumeA.getAmount_F().add(volumeB.getAmount_F()) due to rounding.
     * @param valueA The value in volumeA, provided for convenience.
     * @param valueB The value in volumeB, provided for convenience.
     * @return The merged value. */
    protected abstract T merge(FluidVolume volumeA, FluidVolume volumeB, FluidAmount amount, T valueA, T valueB);

    // ################
    // Tooltips
    // ################

    /** Adds tooltip extras for this fluid property when getting the tooltip for just the {@link FluidKey}. */
    public void addTooltipExtras(FluidKey fluid, FluidTooltipContext context, List<Text> tooltip) {
        if (context.isAdvanced()) {
            tooltip.add(new TranslatableText("libblockattributes.fluid_property.advanced_prefix_key", nbtKey));
        }
    }

    /** Adds tooltip extras for this fluid property when getting the tooltip for a full {@link FluidVolume}. */
    public void addTooltipExtras(FluidVolume fluid, FluidTooltipContext context, List<Text> tooltip) {
        if (context.isAdvanced()) {
            tooltip.add(new TranslatableText("libblockattributes.fluid_property.advanced_prefix_value", get(fluid)));
        }
    }
}
