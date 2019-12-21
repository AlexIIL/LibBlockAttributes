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
     *         {@link #toTag(CompoundTag)} and {@link #WeightedFluidVolume(WeightedFluidKey, CompoundTag)}, so you can
     *         move things around before these are called. */
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
