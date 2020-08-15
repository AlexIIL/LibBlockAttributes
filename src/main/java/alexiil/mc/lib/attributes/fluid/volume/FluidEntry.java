/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.potion.Potion;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

/** {@link Identifier} equivalent for {@link FluidKey}. The only two permitted sub-types are:
 * <ol>
 * <li>{@link FluidRegistryEntry}: for fluids that are based on a minecraft object that's registered in a
 * {@link Registry}.</li>
 * <li>{@link FluidFloatingEntry}: for fluids that aren't based an a minecraft object, and so are identified by a
 * string/identifier.</li>
 * </ol>
 * All {@link FluidEntry} to {@link FluidKey} mappings are stored in {@link FluidKeys}. */
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

    public abstract void toMcBuffer(PacketByteBuf buffer);

    public static FluidEntry fromMcBuffer(PacketByteBuf buffer) {
        byte type = buffer.readByte();
        if (type == 0) {
            return new FluidFloatingEntry(buffer.readIdentifier());
        }
        Registry<?> registry;
        if (type == 1) {
            registry = Registry.FLUID;
        } else if (type == 2) {
            registry = Registry.POTION;
        } else {
            assert type == 3 : "Unknown remote FluidEntry type " + type;
            Identifier id = buffer.readIdentifier();
            registry = Registry.REGISTRIES.get(id);
            if (registry == null) {
                throw new IllegalArgumentException("Unknown remote registry " + id);
            }
        }
        return read0(buffer, registry);
    }

    // TODO: LNS NetByteBuf read/write!

    private static <T> FluidEntry read0(PacketByteBuf buffer, Registry<T> registry) {
        Identifier id = buffer.readIdentifier();
        T obj = registry.get(id);
        if (obj == null) {
            throw new IllegalArgumentException(
                "Unknown remote object " + id + " in registry " + FluidRegistryEntry.getName(registry)
            );
        }
        return new FluidRegistryEntry<>(registry, obj);
    }

    /** @return True if this corresponds to the default value in the backing registry. (No floating entries are
     *         empty). */
    public abstract boolean isEmpty();

    /** @return The LBA-internal name used to serialise this entry. Floating entries return "i", {@link Fluid}-based
     *         registry entries return "f", {@link Potion}-based registry entries return "p", and any other registry
     *         entry returns the ID of that registry. */
    public abstract String getRegistryInternalName();

    /** @return The {@link Identifier} that the backing object uses. */
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
        public void toMcBuffer(PacketByteBuf buffer) {
            buffer.writeByte(0);
            buffer.writeIdentifier(id);
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
