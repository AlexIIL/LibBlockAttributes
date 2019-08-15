/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/** A fluid that changes it's makup based on the {@link Biome}s that it is taken from. */
public class BiomeSourcedFluidVolume extends NormalFluidVolume {

    private final Object2IntMap<Biome> biomeSources = new Object2IntOpenHashMap<>();

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey fluid, int amount) {
        this(fluid, Biomes.DEFAULT, amount);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey fluid, Biome source, int amount) {
        super(fluid, amount);
        biomeSources.put(source, amount);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey fluid, CompoundTag tag) {
        super(fluid, tag);

        int total = 0;
        ListTag biomes = tag.getList("biomes", new CompoundTag().getType());
        for (int i = 0; i < biomes.size(); i++) {
            CompoundTag biomeTag = biomes.getCompoundTag(i);
            Biome biome = Registry.BIOME.get(Identifier.tryParse(biomeTag.getString("Name")));
            int amount = biomeTag.getInt("Amount");
            if (amount < 1) {
                amount = 1;
            }
            if (amount + total > getAmount()) {
                amount = getAmount() - total;
                if (amount <= 0) {
                    break;
                }
            }
            if (biome == null) {
                biome = Biomes.DEFAULT;
            }
            total += amount;
            int currentAmount = biomeSources.getOrDefault(biome, 0);
            biomeSources.put(biome, currentAmount + amount);
        }
        int missing = getAmount() - total;
        assert missing >= 0;
        if (missing > 0) {
            int currentAmount = biomeSources.getOrDefault(Biomes.DEFAULT, 0);
            biomeSources.put(Biomes.DEFAULT, currentAmount + missing);
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        tag = super.toTag(tag);
        ListTag biomeList = new ListTag();
        for (Biome biome : biomeSources.keySet()) {
            CompoundTag biomeTag = new CompoundTag();
            Identifier id = Registry.BIOME.getId(biome);
            if (id == null) {
                // The fromTag() method will sort this out
                continue;
            }
            biomeTag.putString("Name", id.toString());
            biomeTag.putInt("Amount", biomeSources.getInt(biome));
            biomeList.add(biomeTag);
        }
        tag.put("biomes", biomeList);
        return tag;
    }

    @Override
    public BiomeSourcedFluidKey getFluidKey() {
        return (BiomeSourcedFluidKey) this.fluidKey;
    }

    @Override
    protected FluidVolume copy0() {
        BiomeSourcedFluidVolume copy = (BiomeSourcedFluidVolume) super.copy0();
        copy.biomeSources.clear();
        copy.biomeSources.putAll(biomeSources);
        return copy;
    }

    @Override
    protected void merge0(FluidVolume vol) {
        BiomeSourcedFluidVolume other = (BiomeSourcedFluidVolume) vol;

        for (Biome biome : other.biomeSources.keySet()) {
            addAmount(biome, other.biomeSources.getInt(biome));
        }
        other.setAmount(0);
        other.biomeSources.clear();
    }

    @Override
    protected BiomeSourcedFluidVolume split0(int toTake) {
        switch (biomeSources.size()) {
            case 0: {
                // If this was empty then the parent's split method wouldn't have called this
                // and if the amount is greater than 0 then it should always have biome sources.
                throw new IllegalStateException("Cannot have 0 biome sources!");
            }
            case 1: {
                Biome biome = biomeSources.keySet().iterator().next();
                int newAmount = getAmount() - toTake;
                biomeSources.put(biome, newAmount);
                setAmount(newAmount);
                return getFluidKey().withAmount(biome, toTake);
            }
            case 2: {
                ObjectIterator<Biome> iterator = biomeSources.keySet().iterator();
                Biome biomeA = iterator.next();
                Biome biomeB = iterator.next();
                int amountA = biomeSources.getInt(biomeA);
                int amountB = biomeSources.getInt(biomeB);

                if (amountB > amountA) {
                    int a = amountA;
                    amountA = amountB;
                    amountB = a;

                    Biome b = biomeA;
                    biomeA = biomeB;
                    biomeB = b;
                }

                int total = amountA + amountB;
                assert total == getAmount();

                int takeAmountB = amountB * toTake / total;
                int rem = amountB * toTake % total;
                int takeAmountA = toTake - takeAmountB;
                if (rem > 0 && takeAmountA > 1) {
                    // The opposite way round to normal to make it bounce around the same ratio
                    if (Math.random() * total < rem) {
                        takeAmountA--;
                        takeAmountB++;
                    }
                }

                if (amountA == takeAmountA) {
                    biomeSources.removeInt(biomeA);
                } else {
                    biomeSources.put(biomeA, amountA - takeAmountA);
                }

                BiomeSourcedFluidVolume other = getFluidKey().withAmount(biomeA, takeAmountA);
                if (takeAmountB > 0) {
                    other.addAmount(biomeB, takeAmountB);
                    if (amountB == takeAmountB) {
                        biomeSources.removeInt(biomeB);
                    } else {
                        biomeSources.put(biomeB, amountB - takeAmountB);
                    }
                }

                setAmount(total - toTake);
                return other;
            }
            default: {
                Biome[] biomes = biomeSources.keySet().toArray(new Biome[0]);
                int[] amounts = biomeSources.values().toIntArray();
                IntComparator comp = (a, b) -> Integer.compare(amounts[a], amounts[b]);
                Swapper swapper = (a, b) -> {
                    Biome biome = biomes[a];
                    biomes[a] = biomes[b];
                    biomes[b] = biome;

                    int amount = amounts[a];
                    amounts[a] = amounts[b];
                    amounts[b] = amount;
                };
                // Dammit fastutil why did you have to use the same name as java :(
                it.unimi.dsi.fastutil.Arrays.quickSort(0, amounts.length, comp, swapper);
                int total = getAmount();
                // assert total = sum(amounts);

                int[] takeAmounts = new int[amounts.length];
                int taken = 0;

                for (int i = 1; i < amounts.length; i++) {
                    taken += (takeAmounts[i] = amounts[i] * toTake / total);
                }
                takeAmounts[0] = toTake - taken;

                BiomeSourcedFluidVolume other = getFluidKey().withAmount(biomes[0], takeAmounts[0]);

                if (amounts[0] == takeAmounts[0]) {
                    biomeSources.removeInt(biomes[0]);
                } else {
                    biomeSources.put(biomes[0], amounts[0] - takeAmounts[0]);
                }

                for (int i = 1; i < amounts.length; i++) {
                    if (takeAmounts[i] > 0) {
                        other.addAmount(biomes[i], takeAmounts[i]);
                        if (amounts[i] == takeAmounts[i]) {
                            biomeSources.removeInt(biomes[i]);
                        } else {
                            biomeSources.put(biomes[i], amounts[i] - takeAmounts[i]);
                        }
                    }
                }

                setAmount(getAmount() - toTake);
                return other;
            }
        }
    }

    /** @return An unmodifiable view of the biome sources map. */
    public Object2IntMap<Biome> getBiomeSources() {
        return Object2IntMaps.unmodifiable(biomeSources);
    }

    public void addAmount(Biome source, int amount) {
        int thisAmount = biomeSources.getOrDefault(source, 0);
        biomeSources.put(source, thisAmount + amount);
        setAmount(getAmount() + amount);
    }

    public void addAmounts(Object2IntMap<Biome> sources) {
        for (Biome biome : sources.keySet()) {
            addAmount(biome, sources.getInt(biome));
        }
    }

    @Override
    public List<Text> getTooltipText(TooltipContext ctx) {
        List<Text> list = super.getTooltipText(ctx);
        if (ctx.isAdvanced()) {
            for (Biome biome : biomeSources.keySet()) {
                int amount = biomeSources.getInt(biome);
                Text text = new LiteralText(amount + " / " + BUCKET + " of ");
                list.add(text.append(biome.getName()).formatted(Formatting.GRAY));
            }
        }
        return list;
    }
}
