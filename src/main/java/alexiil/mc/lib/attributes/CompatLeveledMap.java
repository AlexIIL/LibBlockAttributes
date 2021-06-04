/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.block.Block;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import alexiil.mc.lib.attributes.CompatLeveledMap.PredicateEntry;
import alexiil.mc.lib.attributes.CompatLeveledMap.PriorityEntry;
import alexiil.mc.lib.attributes.fluid.FluidContainerRegistry;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

/** Used primarily by {@link Attribute} to manage the custom adder list, and {@link FluidContainerRegistry} to manage
 * filters for fluids.
 * 
 * @param <Instance> The object to map directly against with equals - for example this might be {@link Block}, or
 *            {@link Item}, or {@link BlockEntityType}, or {@link EntityType}.
 * @param <Cls> The class to map directly or hierarchically against - for example this might be {@link Block}, or
 *            {@link Item}, or {@link BlockEntity}, or {@link Entity}. */
public final class CompatLeveledMap<Instance, Cls, V> {

    public static final int NULL_PRIORITY = 1 << 16;

    private static final boolean RECORD_ADDITIONS
        = Boolean.getBoolean("libblockattributes.debug.record_attribute_additions");

    // HashMap rather than a ClassValue because the target classes
    // (Block, Item, etc) never unload.
    private static final Map<Class<?>, List<Class<?>>> CLASS_TO_SUPERS = new HashMap<>();

    private final String name;
    private final Class<Cls> usedClass;
    private final ValueEntry<V> nullEntry;
    private final Function<Instance, String> toStringFunc;

    public int baseOffset = 0;
    public int priorityMultiplier = 1;

    // fields rather than an enum map because there's only 2 options
    // and adding types shouldn't happen lightly.

    // Most of these (more complex) fields are null before use
    // as there's a lot of them, but most of them will only be used rarely

    /** {@link AttributeSourceType#INSTANCE} */
    private PriorityEntry instanceValues = null;

    /** {@link AttributeSourceType#COMPAT_WRAPPER} */
    private PriorityEntry compatValues = null;

    private Map<Instance, ValueEntry<V>> resolved = null;
    private Map<Class<?>, ValueEntry<V>> classResolved = null;

    /** Set to true when a target has been resolved by its class rather than its instance. */
    private boolean resolvedByClass = false;

    /** @param nullValue A non-null value to use to indicate that this doesn't contain any entries for the given key.
     *            Note that this value will not be returned unless it is added to this map separately with any of the
     *            "put" methods. */
    public CompatLeveledMap(String name, Class<Cls> usedClass, V nullValue, Function<Instance, String> toStringFunc) {
        this.name = name;
        this.usedClass = usedClass;
        this.nullEntry = new ValueEntry<>(nullValue, NULL_PRIORITY);
        this.toStringFunc = toStringFunc;
    }

    @Nullable
    public V get(Instance key, Class<? extends Cls> clazz) {
        ValueEntry<V> value = getEntry(key, clazz);
        return value != nullEntry ? value.value : null;
    }

    public ValueEntry<V> getEntry(Instance key, Class<? extends Cls> clazz) {
        ValueEntry<V> value = null;
        if (resolved != null) {
            value = resolved.get(key);
            if (value != null && value.priority == 0) {
                return value;
            }
        }
        if (classResolved != null) {
            ValueEntry<V> instance = value;
            value = classResolved.get(clazz);
            if (value == null) {
                if (instance != null) {
                    return instance;
                }
            } else {
                if (instance == null) {
                    return value;
                }
                return value.priority < instance.priority ? value : instance;
            }
        }
        if (instanceValues != null) {
            value = instanceValues.get(key, clazz);
            if (value != null) {
                return resolvedByClass ? resolveClassTo(clazz, value) : resolveTo(key, value);
            }
        }
        if (compatValues != null) {
            value = compatValues.get(key, clazz);
            if (value != null) {
                return resolvedByClass ? resolveClassTo(clazz, value) : resolveTo(key, value);
            }
        }
        resolveTo(key, nullEntry);
        return nullEntry;
    }

    private ValueEntry<V> resolveTo(Instance key, ValueEntry<V> entry) {
        if (resolved == null) {
            resolved = new HashMap<>();
        }
        resolved.put(key, entry);
        return entry;
    }

    private ValueEntry<V> resolveClassTo(Class<? extends Cls> key, ValueEntry<V> entry) {
        if (classResolved == null) {
            classResolved = new HashMap<>();
        }
        classResolved.put(key, entry);
        return entry;
    }

    private static Iterable<Class<?>> classesToConsider(Class<?> clazz) {
        List<Class<?>> list = CLASS_TO_SUPERS.get(clazz);
        if (list != null) {
            return list;
        }

        Set<Class<?>> classes = new LinkedHashSet<>();
        Class<?> s = clazz;
        do {
            classes.add(s);
            Collections.addAll(classes, s.getInterfaces());
            for (Class<?> cls : s.getInterfaces()) {
                for (Class<?> c2 : classesToConsider(cls)) {
                    classes.add(c2);
                }
            }
        } while ((s = s.getSuperclass()) != null);

        list = new ArrayList<>(classes);
        CLASS_TO_SUPERS.put(clazz, list);
        return list;
    }

    public void putExact(AttributeSourceType type, Instance key, V value) {
        if (resolved != null) {
            resolved.remove(key);
        }

        PriorityEntry entry = getOrCreateEntry(type);
        if (entry.exactMappings == null) {
            entry.exactMappings = new HashMap<>();
            if (RECORD_ADDITIONS) {
                entry.exactMappingsTrace = new HashMap<>();
            }
        }

        V old = entry.exactMappings.put(key, value);
        if (old != null) {
            LibBlockAttributes.LOGGER.warn(
                "Replaced the " + name + " value for " + toStringFunc.apply(key) + " with " + value + " (was " + old
                    + ")"
            );
            if (RECORD_ADDITIONS) {
                LibBlockAttributes.LOGGER.warn(" - Original added: ", entry.exactMappingsTrace.get(key));
                LibBlockAttributes.LOGGER.warn(" - Replacement: ", new Throwable());
            } else {
                LibBlockAttributes.LOGGER.info(
                    "Try adding '-Dlibblockattributes.debug.record_attribute_additions=true' to your vm arguments to debug the cause of this"
                );
            }
        }

        if (RECORD_ADDITIONS) {
            entry.exactMappingsTrace.put(key, new Throwable());
        }
    }

    public void addPredicateBased(
        AttributeSourceType type, boolean specific, Predicate<? super Instance> predicate, V value
    ) {
        if (specific) {
            addSpecificPredicateBased(type, predicate, value);
        } else {
            addGeneralPredicateBased(type, predicate, value);
        }
    }

    private void addSpecificPredicateBased(AttributeSourceType type, Predicate<? super Instance> predicate, V value) {
        clearResolved();

        PriorityEntry entry = getOrCreateEntry(type);
        if (entry.specificPredicates == null) {
            entry.specificPredicates = new ArrayList<>();
        }
        entry.specificPredicates.add(new PredicateEntry<>(predicate, value));
    }

    public void putClassBased(AttributeSourceType type, Class<?> clazz, boolean matchSubclasses, V value) {

        if (!matchSubclasses) {
            if (clazz.isInterface()) {
                throw new IllegalArgumentException(
                    "The given " + clazz + " is an interface, and matchSubclasses is set to false - "
                        + "which will never match anything, as it's impossible to construct an interface."
                );
            }
            if ((clazz.getModifiers() & Modifier.ABSTRACT) != 0) {
                throw new IllegalArgumentException(
                    "The given " + clazz + " is abstract, and matchSubclasses is set to false - "
                        + "which will never match anything, as it's impossible to construct an abstract class."
                );
            }
        }

        if (clazz.isAssignableFrom(usedClass)) {
            throw new IllegalArgumentException(
                "The given " + clazz + " is a superclass/superinterface of the base " + usedClass
                    + " - which won't work very well, because it will override everything else."
            );
        }

        clearResolved();

        PriorityEntry entry = getOrCreateEntry(type);
        final Map<Class<?>, V> map;
        final Map<Class<?>, Throwable> mapTrace;
        if (matchSubclasses) {
            if (entry.inheritClassMappings == null) {
                entry.inheritClassMappings = new HashMap<>();
                if (RECORD_ADDITIONS) {
                    entry.inheritClassMappingsTrace = new HashMap<>();
                }
            }
            map = entry.inheritClassMappings;
            mapTrace = entry.inheritClassMappingsTrace;
        } else {
            if (entry.exactClassMappings == null) {
                entry.exactClassMappings = new HashMap<>();
                if (RECORD_ADDITIONS) {
                    entry.exactClassMappingsTrace = new HashMap<>();
                }
            }
            map = entry.exactClassMappings;
            mapTrace = entry.exactClassMappingsTrace;
        }
        V old = map.put(clazz, value);
        if (old != null) {
            LibBlockAttributes.LOGGER
                .warn("Replaced the " + name + " value for " + clazz + " with " + value + " (was " + old + ")");
            if (RECORD_ADDITIONS) {
                LibBlockAttributes.LOGGER.warn(" - Original added: ", mapTrace.get(clazz));
                LibBlockAttributes.LOGGER.warn(" - Replacement: ", new Throwable());
            } else {
                LibBlockAttributes.LOGGER.info(
                    "Try adding '-Dlibblockattributes.debug.record_attribute_additions=true' to your vm arguments to debug the cause of this"
                );
            }
        }

        if (RECORD_ADDITIONS) {
            mapTrace.put(clazz, new Throwable());
        }
    }

    private void addGeneralPredicateBased(AttributeSourceType type, Predicate<? super Instance> predicate, V value) {
        clearResolved();

        PriorityEntry entry = getOrCreateEntry(type);
        if (entry.generalPredicates == null) {
            entry.generalPredicates = new ArrayList<>();
        }
        entry.generalPredicates.add(new PredicateEntry<>(predicate, value));
    }

    private PriorityEntry getOrCreateEntry(AttributeSourceType type) {
        switch (type) {
            case INSTANCE: {
                if (instanceValues == null) {
                    instanceValues = new PriorityEntry(8 * (baseOffset + priorityMultiplier * type.ordinal()));
                }
                return instanceValues;
            }
            case COMPAT_WRAPPER: {
                if (compatValues == null) {
                    compatValues = new PriorityEntry(8 * (baseOffset + priorityMultiplier * type.ordinal()));
                }
                return compatValues;
            }
            default: {
                throw new IllegalArgumentException("Unknown AttributeSourceType" + type + "!");
            }
        }
    }

    private void clearResolved() {
        resolved = null;
        classResolved = null;
    }

    static final class PredicateEntry<K, V> {
        final Predicate<? super K> predicate;
        final V value;

        PredicateEntry(Predicate<? super K> predicate, V value) {
            this.predicate = predicate;
            this.value = value;
        }
    }

    public static final class ValueEntry<V> {
        public final V value;

        /**
         * <ol>
         * <li>0-7 for {@link AttributeSourceType#INSTANCE}</li>
         * <li>8-15 for {@link AttributeSourceType#COMPAT_WRAPPER}</li>
         * <li>{@link #NULL_PRIORITY} for missing entry.</li>
         * </ol>
         */
        public final int priority;

        ValueEntry(V value, int priority) {
            this.value = value;
            this.priority = priority;
        }
    }

    final class PriorityEntry {
        private final int basePriority;

        private Map<Instance, V> exactMappings = null;
        private List<PredicateEntry<Instance, V>> specificPredicates = null;
        private Map<Class<?>, V> exactClassMappings = null;
        private Map<Class<?>, V> inheritClassMappings = null;
        private List<PredicateEntry<Instance, V>> generalPredicates = null;

        private Map<Instance, Throwable> exactMappingsTrace = null;
        private Map<Class<?>, Throwable> exactClassMappingsTrace = null;
        private Map<Class<?>, Throwable> inheritClassMappingsTrace = null;

        PriorityEntry(int basePriority) {
            this.basePriority = basePriority;
        }

        @Nullable
        ValueEntry<V> get(Instance key, Class<? extends Cls> clazz) {
            resolvedByClass = false;
            V value;
            if (exactMappings != null) {
                value = exactMappings.get(key);
                if (value != null) {
                    return new ValueEntry<>(value, basePriority);
                }
            }
            if (specificPredicates != null) {
                for (PredicateEntry<Instance, V> entry : specificPredicates) {
                    if (entry.predicate.test(key)) {
                        return new ValueEntry<>(entry.value, basePriority + 1);
                    }
                }
            }
            if (exactClassMappings != null) {
                value = exactClassMappings.get(clazz);
                if (value != null) {
                    resolvedByClass = true;
                    return new ValueEntry<>(value, basePriority + 2);
                }
            }
            if (inheritClassMappings != null) {
                for (Class<?> cls : classesToConsider(clazz)) {
                    value = inheritClassMappings.get(cls);
                    if (value != null) {
                        resolvedByClass = true;
                        return new ValueEntry<>(value, basePriority + 3);
                    }
                }
            }
            if (generalPredicates != null) {
                for (PredicateEntry<Instance, V> entry : generalPredicates) {
                    if (entry.predicate.test(key)) {
                        return new ValueEntry<>(entry.value, basePriority + 4);
                    }
                }
            }
            return null;
        }
    }
}
