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
import alexiil.mc.lib.attributes.AdderList.PredicateEntry;
import alexiil.mc.lib.attributes.AdderList.PriorityEntry;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

/** Used by {@link Attribute} to manage the custom adder list. */
final class AdderList<Target, Adder> {

    private static final boolean RESOLVE = true;

    // HashMap rather than a ClassValue because the target classes
    // (Block, Item, etc) never unload.
    private static final Map<Class<?>, List<Class<?>>> CLASS_TO_SUPERS = new HashMap<>();

    private final String name;
    private final Class<Target> targetClass;
    private final Adder nullValue;
    private final Function<Target, String> toStringFunc;

    // fields rather than an enum map because there's only 2 options
    // and adding types shouldn't happen lightly.

    // Most of these (more complex) fields are null before use
    // as there's a lot of them, but most of them will only be used rarely

    /** {@link AttributeSourceType#INSTANCE} */
    private PriorityEntry instanceValues = null;

    /** {@link AttributeSourceType#COMPAT_WRAPPER} */
    private PriorityEntry compatValues = null;

    private Map<Target, Adder> resolved = null;

    /** @param nullValue A non-null value to use to indicate that this doesn't contain any entries for the given key.
     *            Note that this value will not be returned unless it is added to this map separately with any of the
     *            "put" methods. */
    public AdderList(String name, Class<Target> targetClass, Adder nullValue, Function<Target, String> toStringFunc) {
        this.name = name;
        this.targetClass = targetClass;
        this.nullValue = nullValue;
        this.toStringFunc = toStringFunc;
    }

    @Nullable
    public Adder get(Target key) {
        Adder value;
        if (resolved != null) {
            value = resolved.get(key);
            if (value != null) {
                if (value == nullValue) {
                    return null;
                }
                return value;
            }
        }
        if (instanceValues != null) {
            value = instanceValues.get(key);
            if (value != null) {
                return resolveTo(key, value);
            }
        }
        if (compatValues != null) {
            value = compatValues.get(key);
            if (value != null) {
                return resolveTo(key, value);
            }
        }
        resolveTo(key, nullValue);
        return null;
    }

    private Adder resolveTo(Target key, Adder value) {
        if (RESOLVE) {
            if (resolved == null) {
                resolved = new HashMap<>();
            }
            resolved.put(key, value);
        }
        return value;
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

    void putExact(AttributeSourceType type, Target key, Adder value) {
        if (resolved != null) {
            resolved.remove(key);
        }

        PriorityEntry entry = getOrCreateEntry(type);
        if (entry.exactMappings == null) {
            entry.exactMappings = new HashMap<>();
        }

        Adder old = entry.exactMappings.put(key, value);
        LibBlockAttributes.LOGGER
            .warn("Replaced the attribute " + name + " value for " + toStringFunc.apply(key) + " with " + value + " (was " + old + ")");
    }

    void addPredicateBased(
        AttributeSourceType type, boolean specific, Predicate<? super Target> predicate, Adder value
    ) {
        if (specific) {
            addSpecificPredicateBased(type, predicate, value);
        } else {
            addGeneralPredicateBased(type, predicate, value);
        }
    }

    private void addSpecificPredicateBased(AttributeSourceType type, Predicate<? super Target> predicate, Adder value) {
        clearResolved();

        PriorityEntry entry = getOrCreateEntry(type);
        if (entry.specificPredicates == null) {
            entry.specificPredicates = new ArrayList<>();
        }
        entry.specificPredicates.add(new PredicateEntry<>(predicate, value));
    }

    void putClassBased(AttributeSourceType type, Class<?> clazz, boolean matchSubclasses, Adder value) {

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

        if (clazz.isAssignableFrom(targetClass)) {
            throw new IllegalArgumentException(
                "The given " + clazz + " is a superclass/superinterface of the base class " + targetClass
                    + " - which won't work very well, because it will override everything else"
            );
        }

        clearResolved();

        PriorityEntry entry = getOrCreateEntry(type);
        final Map<Class<?>, Adder> map;
        if (matchSubclasses) {
            if (entry.inheritClassMappings == null) {
                entry.inheritClassMappings = new HashMap<>();
            }
            map = entry.inheritClassMappings;

        } else {
            if (entry.exactClassMappings == null) {
                entry.exactClassMappings = new HashMap<>();
            }
            map = entry.exactClassMappings;

        }
        Adder old = map.put(clazz, value);
        if (old != null) {
            LibBlockAttributes.LOGGER.warn(
                "Replaced the attribute " + name + " value for " + clazz + " with " + value + " (was " + old + ")"
            );
        }
    }

    private void addGeneralPredicateBased(AttributeSourceType type, Predicate<? super Target> predicate, Adder value) {
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
                    instanceValues = new PriorityEntry();
                }
                return instanceValues;
            }
            case COMPAT_WRAPPER: {
                if (compatValues == null) {
                    compatValues = new PriorityEntry();
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
    }

    static final class PredicateEntry<K, V> {
        final Predicate<? super K> predicate;
        final V value;

        public PredicateEntry(Predicate<? super K> predicate, V value) {
            this.predicate = predicate;
            this.value = value;
        }
    }

    final class PriorityEntry {
        private Map<Target, Adder> exactMappings = null;
        private List<PredicateEntry<Target, Adder>> specificPredicates = null;
        private Map<Class<?>, Adder> exactClassMappings = null;
        private Map<Class<?>, Adder> inheritClassMappings = null;
        private List<PredicateEntry<Target, Adder>> generalPredicates = null;

        @Nullable
        Adder get(Target key) {
            Adder value;
            if (exactMappings != null) {
                value = exactMappings.get(key);
                if (value != null) {
                    return value;
                }
            }
            if (specificPredicates != null) {
                for (PredicateEntry<Target, Adder> entry : specificPredicates) {
                    if (entry.predicate.test(key)) {
                        return entry.value;
                    }
                }
            }
            if (exactClassMappings != null) {
                value = exactMappings.get(key.getClass());
                if (value != null) {
                    return value;
                }
            }
            if (inheritClassMappings != null) {
                for (Class<?> cls : classesToConsider(key.getClass())) {
                    value = inheritClassMappings.get(cls);
                    if (value != null) {
                        return value;
                    }
                }
            }
            if (generalPredicates != null) {
                for (PredicateEntry<Target, Adder> entry : generalPredicates) {
                    if (entry.predicate.test(key)) {
                        return entry.value;
                    }
                }
            }
            return null;
        }
    }
}
