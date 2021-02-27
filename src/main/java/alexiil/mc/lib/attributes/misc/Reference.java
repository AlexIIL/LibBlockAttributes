/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import alexiil.mc.lib.attributes.Simulation;

/** A reference to some object. The object can either be obtained from ({@link #get()}), or changed with
 * ({@link #set(Object)}). Note that changing the object isn't always permitted, and so it may return false if no change
 * happened.
 * <p>
 * Most users will only need to construct {@link Ref} instances to create a {@link Reference}, which is a simple
 * reference that stores the object in a public modifiable field. However there are also 4 other basic implementations:
 * <ul>
 * <li>{@link UnmodifiableRef} is a Reference which always returns the same object from {@link #get()}, and returns
 * false from {@link #set(Object)} and {@link #isValid(Object)}. You can either construct instances directly or via the
 * factory method {@link Reference#unmodifiable(Object)}.</li>
 * <li>{@link CallableRef} is a Reference which delegates to 3 objects: a {@link Supplier} for {@link #get()}, a
 * {@link Consumer} for {@link #set(Object)}, and a {@link Predicate} for {@link #isValid(Object)}. You can either
 * construct instances directly, or use the factory method {@link Reference#callable(Supplier, Consumer, Predicate)}.
 * </li>
 * <li>{@link SimulatableRef} is similar to {@link CallableRef}, but uses a single {@link LimitedConsumer} for both
 * {@link #set(Object)} and {@link #isValid(Object)}.</li>
 * <li>{@link DestroyableRef} is a bit different from the others, as it wraps another {@link Reference} internally, and
 * delegates all calls to it. However you can call {@link DestroyableRef#destroy()} to disallow any further
 * modifications to the backing reference.</li>
 * </ul>
 */
public interface Reference<T> {

    /** @return The object referenced. Note that you should generally not modify the returned value directly - instead
     *         copy it before passing it to {@link #set(Object)} or {@link #isValid(Object)} to see if your
     *         modifications are permitted. */
    T get();

    /** @return True if the new value was accepted, false otherwise. */
    boolean set(T value);

    /** @return True if {@link #set(Object)} was called with the same value. */
    boolean isValid(T value);

    /** Delegates to {@link #set(Object)} if the simulation is {@link Simulation#ACTION}, otherwise it delegates to
     * {@link #isValid(Object)}. */
    default boolean set(T value, Simulation simulation) {
        if (simulation == Simulation.ACTION) {
            return set(value);
        } else {
            return isValid(value);
        }
    }

    /** @return A {@link DestroyableRef} that can be modified until {@link DestroyableRef#destroy()} is called, after
     *         which all calls to {@link #set(Object)} and {@link #isValid(Object)} will return false. */
    default DestroyableRef<T> asDestroyable() {
        return new DestroyableRef<>(this);
    }

    /** @return A new {@link Reference} that doesn't permit modifications to the given object. */
    public static <T> UnmodifiableRef<T> unmodifiable(T obj) {
        return new UnmodifiableRef<>(obj);
    }

    /** @param getter The {@link Supplier} which is used for {@link #get()}.
     * @param setter The {@link Consumer} which is used for {@link #set(Object)} (after the filter).
     * @param filter The {@link Predicate} which is used to test inputs for both {@link #set(Object)} and
     *            {@link #isValid(Object)}.
     * @return A new {@link CallableRef} that delegates to the getter, setter, and filter for all of the functions. */
    public static <T> CallableRef<T> callable(Supplier<T> getter, Consumer<T> setter, Predicate<T> filter) {
        return new CallableRef<>(getter, setter, filter);
    }

    /** @param getter The {@link Supplier} which is used for {@link #get()}
     * @param setter The {@link LimitedConsumer} which is used for {@link #set(Object)}and {@link #isValid(Object)}.
     * @return A new {@link SimulatableRef} that delegates to the getter and setter for all of it's functions. */
    public static <T> SimulatableRef<T> simulating(Supplier<T> getter, LimitedConsumer<T> setter) {
        return new SimulatableRef<>(getter, setter);
    }

    /** @return A new {@link DestroyableRef} that delegates to the given reference for getting, although setting only
     *         succeeds until {@link DestroyableRef#destroy()} is called. */
    public static <T> DestroyableRef<T> destroying(Reference<T> ref) {
        return new DestroyableRef<>(ref);
    }
}
