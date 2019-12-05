package alexiil.mc.lib.attributes.fluid.volume;

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

import alexiil.mc.lib.attributes.fluid.amount.BigFluidAmount;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;

public abstract class WeightedFluidVolume<T> extends FluidVolume {

    private final WeightedFluidKey<T> key;
    private final Map<T, FluidAmount> values = new HashMap<>();

    public WeightedFluidVolume(WeightedFluidKey<T> key, T value, FluidAmount amount) {
        super(key, amount);
        this.key = key;
        values.put(value, amount);
    }

    public WeightedFluidVolume(WeightedFluidKey<T> key, CompoundTag tag) {
        super(key, tag);
        this.key = key;

        final T _def = defaultValue();
        FluidAmount used = FluidAmount.ZERO;

        ListTag list = tag.getList(saveName(), new CompoundTag().getType());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ctag = list.getCompound(i);
            FluidAmount amount;
            if (ctag.contains(KEY_AMOUNT_1620INT)) {
                amount = FluidAmount.of1620(ctag.getInt(KEY_AMOUNT_1620INT));
            } else {
                amount = FluidAmount.fromNbt(ctag.getCompound(KEY_AMOUNT_LBA_FRACTION));
            }
            T value = readValue(tag);
            if (value == null) {
                value = _def;
            }

            FluidAmount current = values.get(value);
            FluidAmount next = used.add(amount);

            if (next.isGreaterThan(getAmount_F())) {
                if (getAmount_F().equals(used)) {
                    break;
                } else {
                    amount = next.sub(getAmount_F());
                }
                used = getAmount_F();
            } else {
                used = next;
            }
            values.put(value, (current == null ? amount : current.add(amount)));
        }

        FluidAmount needed = getAmount_F().sub(used);
        if (needed.isPositive()) {
            values.put(_def, needed.add(values.get(_def)));
        } else {
            assert needed.isZero() : "leftover wasn't zero! (" + needed + ", Am=" + getAmount_F() + ")";
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
    protected WeightedFluidVolume<T> split0(FluidAmount toTake) {
        switch (values.size()) {
            case 0: {
                // If this was empty then the parent's split method wouldn't have called this
                // and if the amount is greater than 0 then it should always have values
                throw new IllegalStateException("Cannot have 0 values!");
            }
            case 1: {
                T biome = values.keySet().iterator().next();
                FluidAmount newAmount = getAmount_F().sub(toTake);
                values.put(biome, newAmount);
                setAmount(newAmount);
                return getFluidKey().withAmount(biome, toTake);
            }
            case 2: {
                Iterator<T> iterator = values.keySet().iterator();
                T valueA = iterator.next();
                T valueB = iterator.next();
                FluidAmount amountA = values.get(valueA);
                FluidAmount amountB = values.get(valueB);

                if (amountB.isGreaterThan(amountA)) {
                    FluidAmount a = amountA;
                    amountA = amountB;
                    amountB = a;

                    T b = valueA;
                    valueA = valueB;
                    valueB = b;
                }

                FluidAmount total = amountA.add(amountB);
                assert total.equals(getAmount_F());

                // For now
                BigFluidAmount takeBigAmountA = amountA.bigMul(toTake).div(total);
                BigFluidAmount takeBigAmountB = toTake.sub(takeBigAmountA);
                assert takeBigAmountB.equals(amountB.bigMul(toTake).div(total));

                final FluidAmount takeAmountA;
                final FluidAmount takeAmountB;

                if (takeBigAmountA.fitsInLongInt() && takeBigAmountB.fitsInLongInt()) {
                    takeAmountA = takeBigAmountA.asLongIntExact();
                    takeAmountB = takeBigAmountB.asLongIntExact();
                } else {
                    // Notes left because otherwise *I* won't be able to understand the maths

                    // Shove some into the other until it fits
                    // how should this work?
                    // So we need to subtract some amount from A, and add to B. (or vice-versa)
                    // and the end result must be a valid (long) fraction? how?

                    // W1+N1/D1
                    // W2+N2+D2
                    // ofc we can ignore "w"
                    // N1/D1
                    // N2/D2
                    // find some value N3 which can be subtracted from N1/D1 and added to N2/D2
                    // which decreases both D1 and D2?

                    // let's say the maximum D is 30.

                    // 3/10 a
                    // 4/10 b
                    // 7/10 total (=a+b)
                    // take 2/10
                    // TBA = 3/10 * 2/10 div 7/10 = 3/10 * 2/7 = 6/70 = 3/35
                    // TBB = 4/10 * 2/10 div 7/10 = 4/10 * 2/7 = 8/70 = 4/35

                    // need an amount D such that TBA has a multiple <= 30
                    // ...but we already know the highest fraction right? it's "toTake?"
                    // so find the highest multiple of "toTake" that's <= 30?
                    // obviously that's 3.
                    // so D will be some multiple of 1/30?
                    // or will the end result be some multiple of 30?
                    // maybe find the difference between 1/35 and 1/30?
                    // = 1/30 - 1/35 = (35-30)/30*35 = 5/1050 = 1/210
                    // ...ok
                    // so now find the closest values to 3/35 and 4/35 that are over 30...
                    // L1/30 U1/30, L2/30 U2/30
                    // 2/30 3/30, 3/30 4/30
                    // (but mathematically?)
                    // 3/35 - 2/30 = (18-14)/210 = 4/210
                    // 3/30 - 3/35 = (21-18)/210 = 3/210
                    // 4/35 - 3/30 = (24-21)/210 = 3/210
                    // 4/30 - 4/35 = (28-24)/210 = 4/210

                    // but generally...

                    // a -> Na/Da
                    // b -> Nb/Db
                    // total -> Nt/Dt (= Na/Da + Nb/Db)
                    // take = Ns/Ds (<Nt/Dt)

                    // takeA = Na/Da * Ns/Ds div (Na/Da + Nb/Db)
                    // = NaNs/DaDs div (Na/Da + Nb/Db)
                    // = NaNs/DaDs div (NaDb/DaDb+NbDa/DbDa)
                    // = NaNs/DaDs div ( (NaDb + NbDa)/DaDb )
                    // = NaNsDaDb/DaDs(NaDb+NbDa)
                    // = NaNsDb/Ds(NaDb+NbDa)

                    // takeB = Nb/Db * Ns/Ds div (Na/Da + Nb/Db)
                    // = NbNs/DbDs div (Na/Da + Nb/Db)
                    // = NbNs/DbDs div (NaDb/DaDb + NbDa/DbDa)
                    // = NbNs/DbDs div ( (NaDb+NbDa)/DbDa )
                    // = NbNsDbDa/DbDs div (NaDb+NbDa)
                    // = NbNsDa/Ds(NaDb+NbDa)

                    // end denominator -> De = Ds * floor(MAX_DENOM/Ds)
                    // De = Ds * floor(MAX_DENOM/Ds)
                    // = Ds(MAX_DENOM/Ds - (MAX_DENOM%Ds)/Ds)
                    // = MAX_DENOM - MAX_DENOM%Ds

                    // D = toTake * floor(MAX_DENOM/toTake)
                    // C = LCM of D1 and D2
                    // (L1*D1)/(D*D1) <= (N1*D)/(D1*D)
                    // L1*D1 <= N1*D
                    // L1 <= N1*D/D1
                    // L1 = floor(N1*D/D1)
                    // U1 = ceil(N1*D/D1)
                    // L2 = floor(N2*D/D2)
                    // U2 = ceil(N2*D/D2)

                    // floor (a / b) = a/b - (a%b)/b

                    // N1/D1 - L1/D
                    // = N1/D1 - floor(N1*D/D1)/D
                    // = N1/D1 - ( (N1*D - (N1*D)%D1) / D1 )/D
                    // = N1/D1 - ( N1/D1 - ((N1*D)%D1) / (D1*D) )
                    // = N1/D1 - N1/D1 + ((N1*D)%D1) / (D1*D)
                    // = ((N1*D)%D1) / (D1*D)

                    // ceil (a / b) = ?
                    // ceil (2/7) = 2/7 + 5/7
                    // ceil ( 25/7) = 25/7 + 3/7
                    // ceil (a/b) = a/b + (b-a%b)/b
                    // which is *technically* wrong for a=b*(an integer)
                    // however this is never the case here

                    // U2/D - N2/D2 = ceil(N2*D/D2)/D - N2/D2
                    // = ( (N2*D)/D2 + ((D2 - (N2*D))%D2)/D2 )/D - N2/D2
                    // = N2*D/(D2*D) + ((D2 - (N2*D))%D2)/(D*D2) - N2/D2
                    // = (D2-N2*D)%D2 / (D*D2)

                    // Assume they are equal

                    // L1 = greatest value for which L1/D <= N1/D1
                    // U1 = smallest value for which U1/D >= N1/D1
                    // L2 = greatest value for which L2/D <= N2/D2
                    // U2 = smallest value for which U2/D >= N2/D2

                    // N1/D1 - L1/D = ?/D
                    // U1/D - N1/D1 = ?/D
                    // N2/D2 - L2/D = ?/D
                    // U2/D - N2/D2 = ?/D

                    BigFluidAmount hcf = takeBigAmountA.hcf(takeBigAmountB);
                    if (takeBigAmountA.isGreaterThan(takeBigAmountB)) {

                    } else {

                    }
                }

                // All of this is wrong?
                FluidAmount takeAmountB = amountB * toTake / total;
                int rem = amountB * toTake % total;
                FluidAmount takeAmountA = toTake - takeAmountB;
                if (rem > 0 && takeAmountA > 1) {
                    // The opposite way round to normal to make it bounce around the same ratio
                    if (Math.random() * total < rem) {
                        takeAmountA--;
                        takeAmountB++;
                    }
                }

                if (amountA.equals(takeAmountA)) {
                    values.remove(valueA);
                } else {
                    FluidAmount newValue = amountA.sub(takeAmountA);
                    assert newValue.isPositive();
                    values.put(valueA, newValue);
                }

                WeightedFluidVolume<T> other = key.withAmount(valueA, takeAmountA);
                if (takeAmountB.isPositive()) {
                    other.addAmount(valueB, takeAmountB);
                    if (amountB.equals(takeAmountB)) {
                        values.remove(valueB);
                    } else {
                        values.put(valueB, amountB.sub(takeAmountB));
                    }
                }

                setAmount(total.sub(toTake));
                return other;
            }
            default: {
                T[] keys = (T[]) values.keySet().toArray();
                FluidAmount[] amounts = values.values().toArray(new FluidAmount[0]);
                IntComparator comp = (a, b) -> amounts[a].compareTo(amounts[b]);
                Swapper swapper = (a, b) -> {
                    T k = keys[a];
                    keys[a] = keys[b];
                    keys[b] = k;

                    FluidAmount amount = amounts[a];
                    amounts[a] = amounts[b];
                    amounts[b] = amount;
                };
                // Dammit fastutil why did you have to use the same name as java :(
                it.unimi.dsi.fastutil.Arrays.quickSort(0, amounts.length, comp, swapper);
                FluidAmount total = getAmount_F();
                // assert total = sum(amounts);

                FluidAmount[] takeAmounts = new FluidAmount[amounts.length];
                int taken = 0;

                for (int i = 1; i < amounts.length; i++) {
                    taken += (takeAmounts[i] = amounts[i] * toTake / total);
                }
                takeAmounts[0] = toTake - taken;

                WeightedFluidVolume<T> other = key.withAmount(keys[0], takeAmounts[0]);

                if (amounts[0] == takeAmounts[0]) {
                    biomeSources.removeInt(keys[0]);
                } else {
                    biomeSources.put(keys[0], amounts[0] - takeAmounts[0]);
                }

                for (int i = 1; i < amounts.length; i++) {
                    if (takeAmounts[i] > 0) {
                        other.addAmount(keys[i], takeAmounts[i]);
                        if (amounts[i] == takeAmounts[i]) {
                            biomeSources.removeInt(keys[i]);
                        } else {
                            biomeSources.put(keys[i], amounts[i] - takeAmounts[i]);
                        }
                    }
                }

                setAmount(getAmount_F().sub(toTake));
                return other;
            }
        }
    }

    @Override
    protected void merge0(FluidVolume vol) {
        WeightedFluidVolume<?> other = (WeightedFluidVolume<?>) vol;

        for (Object value : other.values.keySet()) {
            addAmount(key.valueClass.cast(value), other.values.get(value));
        }
        other.setAmount(FluidAmount.ZERO);
        other.values.clear();
    }

    public void addAmount(T value, FluidAmount amount) {
        if (amount == null || amount.isZero()) {
            return;
        }
        FluidAmount added = amount.add(values.get(value));
        if (added.isNegative()) {
            throw new IllegalArgumentException("Cannot add to make a negative amount!");
        }

        if (added.isZero()) {
            values.remove(added);
        } else {
            values.put(value, added);
        }
        setAmount(getAmount_F().add(amount));
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
