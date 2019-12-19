/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

public abstract class FluidEntry {
    static final String KEY_REGISTRY_TYPE = "Registry";
    static final String KEY_OBJ_IDENTIFIER = "ObjName";

    private static final Map<Identifier, FloatingEntry> FLOATING = new HashMap<>();

    /* package-private */ FluidEntry() {}

    public static FluidEntry ofId(Identifier id) {
        return FLOATING.computeIfAbsent(id, FloatingEntry::new);
    }

    public abstract void toTag(CompoundTag tag);

    public static FluidEntry fromTag(CompoundTag tag) {
        String str = tag.getString(KEY_REGISTRY_TYPE);
        if (str.equals("i")) {
            FloatingEntry entry = FLOATING.get(Identifier.tryParse(tag.getString(KEY_OBJ_IDENTIFIER)));
            if (entry == null) {
                return FluidKeys.EMPTY.entry;
            }
            return entry;
        }
        DefaultedRegistry<?> registry = FluidRegistryEntry.fromName(str);
        if (registry == null) {
            // The registry that contains the empty fluid
            registry = Registry.FLUID;
        }
        String name = tag.getString(KEY_OBJ_IDENTIFIER);
        return FluidRegistryEntry.fromTag0(registry, name);
    }

    public abstract boolean isEmpty();

    public abstract String getRegistryInternalName();

    public abstract Identifier getId();

    /* package-private */ static final class FloatingEntry extends FluidEntry {
        public final Identifier id;

        private FloatingEntry(Identifier id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null) return false;
            if (obj instanceof FloatingEntry) {
                return id.equals(((FloatingEntry) obj).id);
            }
            return false;
        }

        @Override
        public void toTag(CompoundTag tag) {
            tag.putString(KEY_REGISTRY_TYPE, "i");
            tag.putString(KEY_OBJ_IDENTIFIER, id.toString());
        }

        @Override
        public String toString() {
            return "{FloatingEntry " + getId() + "}";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public String getRegistryInternalName() {
            return "i";
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
}
