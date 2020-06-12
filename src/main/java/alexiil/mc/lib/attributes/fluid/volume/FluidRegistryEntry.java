/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

public final class FluidRegistryEntry<T> extends FluidEntry {

    final Registry<T> backingRegistry;
    final T backingObject;
    final Identifier objId;

    public FluidRegistryEntry(DefaultedRegistry<T> backingRegistry, T backingObject) {
        this((Registry<T>) backingRegistry, backingObject);
    }

    public FluidRegistryEntry(Registry<T> backingRegistry, T backingObject) {
        this(backingRegistry, backingObject, backingRegistry.getId(backingObject));
    }

    private FluidRegistryEntry(Registry<T> backingRegistry, T backingObject, Identifier objId) {
        super(computeHash(backingRegistry, backingObject, objId));
        if (backingRegistry == null) {
            throw new NullPointerException("backingRegistry");
        }
        if (backingObject == null) {
            throw new NullPointerException("backingObject");
        }
        if (getName(backingRegistry) == null) {
            throw new IllegalArgumentException(
                "You cannot use the " + backingRegistry
                    + " with this because it's not registered with the main registry!"
            );
        }
        this.backingRegistry = backingRegistry;
        this.backingObject = backingObject;
        this.objId = objId;
    }

    private static <T> int computeHash(Registry<T> backingRegistry, T obj, Identifier objId) {
        if (objId == null) {
            throw new IllegalArgumentException(
                "You cannot use " + obj + " with this because it's not registered with " + backingRegistry
                    + " registry!"
            );
        }
        return System.identityHashCode(backingRegistry) * 31 + objId.hashCode();
    }

    static String getName(Registry<?> registry) {
        if (registry == Registry.FLUID) {
            return "f";
        } else if (registry == Registry.POTION) {
            return "p";
        } else {
            Identifier id = ((Registry<Registry<?>>) Registry.REGISTRIES).getId(registry);
            if (id == null) {
                throw new IllegalArgumentException("Unregistered registry: " + registry);
            }
            return id.toString();
        }
    }

    @Nullable
    static DefaultedRegistry<?> fromName(String name) {
        if ("f".equals(name)) {
            return Registry.FLUID;
        } else if ("p".equals(name)) {
            return Registry.POTION;
        } else {
            Identifier id = Identifier.tryParse(name);
            Registry<?> registry = Registry.REGISTRIES.get(id);
            if (registry instanceof DefaultedRegistry<?>) {
                return (DefaultedRegistry<?>) registry;
            }
            return null;
        }
    }

    static <T> FluidRegistryEntry<T> fromTag0(DefaultedRegistry<T> registry, String name) {
        T obj = registry.get(Identifier.tryParse(name));
        return new FluidRegistryEntry<>(registry, obj);
    }

    @Override
    public void toTag(CompoundTag tag) {
        Identifier objId = getId();
        if (objId == null) {
            return;
        } else if (backingRegistry instanceof DefaultedRegistry<?>) {
            if (objId.equals(((DefaultedRegistry<T>) backingRegistry).getDefaultId())) {
                return;
            }
        }
        tag.putString(KEY_REGISTRY_TYPE, getName(backingRegistry));
        tag.putString(KEY_OBJ_IDENTIFIER, objId.toString());
    }

    @Override
    public boolean isEmpty() {
        Identifier objId = getId();
        if (objId == null) {
            return true;
        } else if (backingRegistry instanceof DefaultedRegistry<?>) {
            if (objId.equals(((DefaultedRegistry<T>) backingRegistry).getDefaultId())) {
                return true;
            }
        }
        return false;
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
        FluidRegistryEntry<?> re = (FluidRegistryEntry<?>) other;
        return backingRegistry == re.backingRegistry && Objects.equals(getId(), other.getId());
    }

    @Override
    public String toString() {
        return "{RegistryEntry " + getRegistryInternalName() + " " + getId() + "}";
    }

    @Override
    public String getRegistryInternalName() {
        return getName(backingRegistry);
    }

    @Override
    public Identifier getId() {
        return objId;
    }
}
