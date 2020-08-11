/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BuiltInBiomes;

/**
 * A fluid that changes it's makup based on the {@link Biome}s that it is taken from.
 */
public class BiomeSourcedFluidVolume extends WeightedFluidVolume<RegistryKey<Biome>> {

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, RegistryKey<Biome> value, FluidAmount amount) {
        super(key, value, amount);
    }

    @Deprecated
    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, RegistryKey<Biome> value, int amount) {
        super(key, value, amount);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, FluidAmount amount) {
        super(key, BuiltInBiomes.OCEAN, amount);
    }

    @Deprecated
    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, int amount) {
        super(key, BuiltInBiomes.OCEAN, amount);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, CompoundTag tag) {
        super(key, tag);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, JsonObject json) {
        super(key, json, BiomeSourcedFluidVolume::biomeFromJson);
    }

    private static RegistryKey<Biome> biomeFromJson(String str) {
        return RegistryKey.of(Registry.BIOME_KEY, new Identifier(str));
    }

    @Override
    protected boolean areJsonValuesCompact() {
        return true;
    }

    @Override
    protected JsonElement toJson(RegistryKey<Biome> value) {
        return new JsonPrimitive(value.getValue().toString());
    }

    @Override
    public BiomeSourcedFluidKey getFluidKey() {
        return (BiomeSourcedFluidKey) this.fluidKey;
    }

    @Override
    protected String saveName() {
        return "biomes";
    }

    @Override
    protected RegistryKey<Biome> readValue(CompoundTag holder) {
        return RegistryKey.of(Registry.BIOME_KEY, Identifier.tryParse(holder.getString("Name")));
    }

    @Override
    protected void writeValue(CompoundTag holder, RegistryKey<Biome> value) {
        holder.putString("Name", value.getValue().toString());
    }
}
