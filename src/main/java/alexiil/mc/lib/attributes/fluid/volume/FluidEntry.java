/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

public abstract class FluidEntry {
    static final String KEY_REGISTRY_TYPE = "Registry";
    static final String KEY_OBJ_IDENTIFIER = "ObjName";

    protected final int hash;

    /* package-private */ FluidEntry(int hash) {
        this.hash = hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) {
            return false;
        }
        FluidEntry other = (FluidEntry) obj;
        if (hash != other.hash) {
            return false;
        }
        return equals(other);
    }

    protected abstract boolean equals(FluidEntry other);

    public abstract void toTag(CompoundTag tag);

    /** Reads a {@link FluidEntry} from the given tag. Note that the returned entry might not map to a
     * {@link FluidKey}. */
    public static FluidEntry fromTag(CompoundTag tag) {
        String str = tag.getString(KEY_REGISTRY_TYPE);
        if (str.equals("i")) {
            Identifier id = Identifier.tryParse(tag.getString(KEY_OBJ_IDENTIFIER));
            if (id == null) {
                return FluidKeys.EMPTY.entry;
            }
            return new FluidFloatingEntry(id);
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

    public static final class FluidFloatingEntry extends FluidEntry {
        public final Identifier id;

        public FluidFloatingEntry(Identifier id) {
            super(id.hashCode());
            this.id = id;
        }

        @Override
        public int hashCode() {
            // For binary backwards compat
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // For binary backwards compat
            return super.equals(obj);
        }

        @Override
        protected boolean equals(FluidEntry other) {
            return id.equals(((FluidFloatingEntry) other).id);
        }

        @Override
        public void toTag(CompoundTag tag) {
            tag.putString(KEY_REGISTRY_TYPE, "i");
            tag.putString(KEY_OBJ_IDENTIFIER, id.toString());
        }

        @Override
        public String toString() {
            return "{FluidFloatingEntry " + getId() + "}";
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
