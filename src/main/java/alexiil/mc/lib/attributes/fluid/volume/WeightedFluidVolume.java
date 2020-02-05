/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount.FluidMergeResult;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount.FluidMergeRounding;

public abstract class WeightedFluidVolume<T> extends FluidVolume {

    private final WeightedFluidKey<T> key;

    /** An empty map indicates that it actually stores one entry: the default value -> one. */
    private final Map<T, FluidAmount> values = new HashMap<>();

    public WeightedFluidVolume(WeightedFluidKey<T> key, T value, FluidAmount amount) {
        super(key, amount);
        this.key = key;
        values.put(value, FluidAmount.ONE);
    }

    @Deprecated
    public WeightedFluidVolume(WeightedFluidKey<T> key, T value, int amount) {
        this(key, value, FluidAmount.of1620(amount));
    }

    public WeightedFluidVolume(WeightedFluidKey<T> key, CompoundTag tag) {
        super(key, tag);
        this.key = key;

        final T _def = defaultValue();
        ListTag list = tag.getList(saveName(), new CompoundTag().getType());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ctag = list.getCompound(i);
            FluidAmount amount;
            if (ctag.contains(KEY_AMOUNT_1620INT)) {
                amount = FluidAmount.of1620(ctag.getInt(KEY_AMOUNT_1620INT));
            } else {
                amount = FluidAmount.fromNbt(ctag.getCompound(KEY_AMOUNT_LBA_FRACTION));
            }
            T value = readValue(ctag);
            if (value == null) {
                value = _def;
            }

            if (amount.isNegative() || amount.isOverflow()) {
                amount = FluidAmount.ZERO;
            }

            values.put(value, amount.roundedAdd(values.get(value)));
        }
        normalize();
    }

    @FunctionalInterface
    public interface WeightedStringGetter<T> {
        /** @return Null if the given value wasn't recognised. */
        @Nullable
        T get(String key);
    }

    @FunctionalInterface
    public interface WeightedJsonGetter<T> {
        T fromJson(JsonElement element) throws JsonSyntaxException;
    }

    /** Json deserialiser that assumes the values are simple and require just a string to deserialise. It is only ever
     * necessary for subclasses to call one of these constructors.
     * 
     * @see #WeightedFluidVolume(WeightedFluidKey, JsonObject, WeightedJsonGetter) */
    public WeightedFluidVolume(WeightedFluidKey<T> key, JsonObject json, WeightedStringGetter<T> keyGetter)
        throws JsonSyntaxException {
        super(key, json);
        this.key = key;

        assert areJsonValuesCompact() : "areJsonValuesCompact is false! (wrong constructor)";

        JsonElement weights = json.get(saveName());
        if (weights != null) {
            if (!weights.isJsonObject()) {
                throw new JsonSyntaxException("Expected '" + saveName() + "' to be an object, but was " + weights);
            }

            for (Entry<String, JsonElement> entry : weights.getAsJsonObject().entrySet()) {
                T value = keyGetter.get(entry.getKey());
                if (value == null) {
                    throw new JsonSyntaxException("Unknown value '" + entry.getKey() + "'");
                }
                FluidAmount amount = FluidVolume.parseAmount(entry.getValue());
                if (values.containsKey(value)) {
                    throw new JsonSyntaxException("Duplicate values for " + entry.getKey());
                }
                values.put(value, amount);
            }
            normalize();
        }
    }

    /** Json deserialiser that assumes the values are complicated, and so need a more complex json element than just a
     * string to deserialise. It is only ever necessary for subclasses to call one of these constructors.
     * 
     * @see #WeightedFluidVolume(WeightedFluidKey, JsonObject, WeightedStringGetter) */
    public WeightedFluidVolume(WeightedFluidKey<T> key, JsonObject json, WeightedJsonGetter<T> keyGetter)
        throws JsonSyntaxException {
        super(key, json);
        this.key = key;

        assert !areJsonValuesCompact() : "areJsonValuesCompact is true! (wrong constructor)";

        JsonElement weights = json.get(saveName());
        if (weights != null) {
            if (!weights.isJsonArray()) {
                throw new JsonSyntaxException(
                    "Expected '" + saveName() + "' to be an array of objects, but was " + weights
                );
            }
            for (JsonElement elem : weights.getAsJsonArray()) {
                if (!elem.isJsonObject()) {
                    throw new JsonSyntaxException(
                        "Expected '" + saveName() + "' to be an array of objects, but found " + elem + " in the array!"
                    );
                }
                JsonObject obj = elem.getAsJsonObject();
                JsonElement value = obj.get("value");
                if (value == null) {
                    throw new JsonSyntaxException("Expected 'value', but got nothing!");
                }
                T val = keyGetter.fromJson(value);
                FluidAmount fa = FluidVolume.parseAmount(obj.get("amount"));
                if (values.containsKey(val)) {
                    throw new JsonSyntaxException("Duplicate values for " + value);
                }
                values.put(val, fa);
            }

            normalize();
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        if (!values.isEmpty()) {
            if (values.size() != 1 || values.keySet().iterator().next() != defaultValue()) {
                if (areJsonValuesCompact()) {
                    JsonObject weights = new JsonObject();
                    json.add(saveName(), weights);
                    for (Entry<T, FluidAmount> entry : values.entrySet()) {
                        weights.addProperty(toJson(entry.getKey()).getAsString(), entry.getValue().toParseableString());
                    }
                } else {
                    JsonArray weights = new JsonArray();
                    json.add(saveName(), weights);
                    for (Entry<T, FluidAmount> entry : values.entrySet()) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("amount", entry.getValue().toParseableString());
                        obj.add("value", toJson(entry.getKey()));
                        weights.add(obj);
                    }
                }
            }
        }
        return json;
    }

    protected final void normalize() {
        FluidAmount total = FluidAmount.ZERO;
        for (FluidAmount amount : values.values()) {
            total = total.roundedAdd(amount);
        }
        normalize(total);
    }

    private final void normalize(FluidAmount ctotal) {
        if (!ctotal.isPositive() || values.isEmpty()) {
            values.clear();
            return;
        }

        Iterator<Entry<T, FluidAmount>> iterator = values.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<T, FluidAmount> entry = iterator.next();
            FluidAmount fa = entry.getValue();
            fa = fa.roundedDiv(ctotal);
            if (fa.isPositive()) {
                entry.setValue(fa);
            } else {
                iterator.remove();
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        ListTag list = new ListTag();
        for (Map.Entry<T, FluidAmount> entry : values.entrySet()) {
            CompoundTag ctag = new CompoundTag();
            ctag.put(KEY_AMOUNT_LBA_FRACTION, entry.getValue().toNbt());
            writeValue(ctag, entry.getKey());
            list.add(ctag);
        }
        tag.put(saveName(), list);
        return tag;
    }

    /** @return The name to use when saving/loading the {@link #values} map to/from disk. This is only used in
     *         {@link #toTag(CompoundTag)}, {@link #WeightedFluidVolume(WeightedFluidKey, CompoundTag)},
     *         {@link #toJson()}, {@link #WeightedFluidVolume(WeightedFluidKey, JsonObject, WeightedStringGetter)}, and
     *         {@link #WeightedFluidVolume(WeightedFluidKey, JsonObject, WeightedJsonGetter)}, so you can move things
     *         around before these are called. */
    protected abstract String saveName();

    /** @return The default value to use if none is present. */
    protected final T defaultValue() {
        return key.defaultValue;
    }

    /** Inverse of {@link #writeValue(CompoundTag, Object)}. */
    protected abstract T readValue(CompoundTag holder);

    /** @param holder The {@link CompoundTag} to store the key in. Either "Amount" or "AmountF" might be overwritten by
     *            this class. (If in doubt it is better to use an embedded compound to store all of the data, using a
     *            key like "Name" or "Value"). */
    protected abstract void writeValue(CompoundTag holder, T value);

    /** @return True if the weights should be serialised to json in "compact" form (where the value is serialised into a
     *         single string, and read back with {@link WeightedStringGetter}), or "full" form (and read back with
     *         {@link WeightedJsonGetter}). */
    protected abstract boolean areJsonValuesCompact();

    /** @return The value, turned into a {@link JsonElement}. If the subclass calls
     *         {@link #WeightedFluidVolume(WeightedFluidKey, JsonObject, WeightedStringGetter)} (and thus returns true
     *         from {@link #areJsonValuesCompact()}) then this must be a {@link JsonPrimitive} with a string, that can
     *         read back with the {@link WeightedStringGetter}. Otherwise there are no limits on what this can return,
     *         other than that the {@link WeightedJsonGetter} should be able to read back the given value from the
     *         returned json. */
    protected abstract JsonElement toJson(T value);

    protected Text getTextFor(T value) {
        return new LiteralText(Objects.toString(value));
    }

    @Override
    public WeightedFluidKey<T> getFluidKey() {
        return key;
    }

    /** @return An unmodifiable view of the values map. */
    public Map<T, FluidAmount> getValues() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    protected WeightedFluidVolume<T> copy0() {
        WeightedFluidVolume<T> copy = key.withAmount(getAmount_F());
        copy.values.clear();
        copy.values.putAll(values);
        return copy;
    }

    @Override
    protected WeightedFluidVolume<T> split0(FluidAmount toTake, RoundingMode rounding) {
        WeightedFluidVolume<T> split = key.withAmount(toTake);
        setAmount(getAmount_F().roundedSub(toTake, rounding));
        split.values.clear();
        split.values.putAll(values);
        return split;
    }

    @Override
    protected void merge0(FluidVolume vol, FluidMergeRounding rounding) {
        WeightedFluidVolume<?> other = (WeightedFluidVolume<?>) vol;

        FluidMergeResult result = FluidAmount.merge(getAmount_F(), other.getAmount_F(), rounding);

        for (Map.Entry<T, FluidAmount> entry : values.entrySet()) {
            entry.setValue(entry.getValue().roundedMul(getAmount_F()));
        }

        for (Map.Entry<?, FluidAmount> entry : other.values.entrySet()) {
            T oKey = key.valueClass.cast(entry.getKey());
            values.put(oKey, entry.getValue().roundedMul(other.getAmount_F()).roundedAdd(values.get(oKey)));
        }

        setAmount(result.merged);
        other.setAmount(result.excess);
        other.values.clear();

        normalize();
    }

    public void addAmount(T value, FluidAmount amount) {
        addAmount(value, amount, FluidMergeRounding.DEFAULT);
    }

    public void addAmount(T value, FluidAmount amount, FluidMergeRounding rounding) {
        if (amount == null || amount.isZero()) {
            return;
        }
        FluidMergeResult result = FluidAmount.merge(getAmount_F(), amount, rounding);
        if (result.excess.equals(amount)) {
            return;
        }
        if (result.merged.isNegative()) {
            throw new IllegalArgumentException("Cannot negate " + amount + " from " + getAmount_F());
        }
        if (result.merged.isZero()) {
            setAmount(FluidAmount.ZERO);
            values.clear();
            return;
        }
        setAmount(result.merged);
        values.put(value, values.get(value).roundedAdd(amount.roundedDiv(getAmount_F()), rounding.rounding));
        normalize();
    }

    public void addAmounts(Map<T, FluidAmount> sources) {
        for (Entry<T, FluidAmount> entry : sources.entrySet()) {
            addAmount(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public List<Text> getTooltipText(TooltipContext ctx) {
        List<Text> list = super.getTooltipText(ctx);
        if (ctx.isAdvanced()) {
            for (Entry<T, FluidAmount> entry : values.entrySet()) {
                Text text = new LiteralText(entry.getKey() + " of ");
                list.add(text.append(getTextFor(entry.getKey())).formatted(Formatting.GRAY));
            }
        }
        return list;
    }
}
