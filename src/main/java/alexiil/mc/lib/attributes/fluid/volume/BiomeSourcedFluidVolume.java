/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** A fluid that changes it's makup based on the {@link Biome}s that it is taken from. */
public class BiomeSourcedFluidVolume extends WeightedFluidVolume<Biome> {

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, Biome value, FluidAmount amount) {
        super(key, value, amount);
    }

    @Deprecated
    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, Biome value, int amount) {
        super(key, value, amount);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, FluidAmount amount) {
        super(key, Biomes.DEFAULT, amount);
    }

    @Deprecated
    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, int amount) {
        super(key, Biomes.DEFAULT, amount);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, CompoundTag tag) {
        super(key, tag);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, JsonObject json) {
        super(key, json, BiomeSourcedFluidVolume::biomeFromJson);
    }

    private static Biome biomeFromJson(String str) {
        Identifier id = Identifier.tryParse(str);
        if (id == null || !Registry.BIOME.containsId(id)) {
            throw new JsonSyntaxException(
                "Unknown biome '" + id + "', it must be one of these: ["
                    + Registry.BIOME.getIds().stream().sorted().map(i -> "\n\t - " + i) + "\n]"
            );
        }
        return Registry.BIOME.get(id);
    }

    @Override
    protected boolean areJsonValuesCompact() {
        return true;
    }

    @Override
    protected JsonElement toJson(Biome value) {
        Identifier idFor = Registry.BIOME.getId(value);
        if (idFor == null) {
            return new JsonPrimitive("minecraft:ocean");
        }
        return new JsonPrimitive(idFor.toString());
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
    protected Biome readValue(CompoundTag holder) {
        return Registry.BIOME.get(Identifier.tryParse(holder.getString("Name")));
    }

    @Override
    protected void writeValue(CompoundTag holder, Biome value) {
        Identifier id = Registry.BIOME.getId(value);
        if (id != null) {
            holder.putString("Name", id.toString());
        }
    }
}
