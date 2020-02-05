/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import java.util.function.Consumer;

import alexiil.mc.lib.attributes.Simulation;

/** A type of {@link Consumer} that may or may not accept a given input. */
@FunctionalInterface
public interface LimitedConsumer<T> {

    /** @param object The object to offer. This interface makes no guarantees about whether this object will be
     *            defensively copied or not - so callers should assume that any objects passed to this may be modified
     *            in any way. Likewise, this has no requirements for what callers may do with the object after it has
     *            been passed in.
     * @param simulation If {@link Simulation#ACTION} then this will modify state (if accepted).
     * @return True if the object would have been accepted. */
    boolean offer(T object, Simulation simulation);

    /** @param object The object to offer. This interface makes no guarantees about whether this object will be
     *            defensively copied or not - so callers should assume that any objects passed to this may be modified
     *            in any way. Likewise, this has no requirements for what callers may do with the object after it has
     *            been passed in
     * @return True if the offer was accepted, or false if nothing happened. */
    default boolean offer(T object) {
        return offer(object, Simulation.ACTION);
    }

    /** @param object The object to test for. This interface makes no guarantees about whether this object will be
     *            defensively copied or not - so callers should assume that any objects passed to this may be modified
     *            in any way. Likewise, this has no requirements for what callers may do with the object after it has
     *            been passed in
     * @return True if the offer was accepted, or false if nothing happened. */
    default boolean wouldAccept(T object) {
        return offer(object, Simulation.SIMULATE);
    }

    /** @return A {@link LimitedConsumer} that rejects all inputs. */
    public static <T> LimitedConsumer<T> rejecting() {
        return (o, s) -> false;
    }

    /** Creates a new {@link LimitedConsumer} that accepts everything, and passes it on to the given
     * {@link Consumer}. */
    public static <T> LimitedConsumer<T> fromConsumer(Consumer<T> consumer) {
        return (obj, simulation) -> {
            if (simulation == Simulation.ACTION) {
                consumer.accept(obj);
            }
            return true;
        };
    }
}
