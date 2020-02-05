/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

/** A {@link Reference} that may be obtained through {@link #get()}, but never allows the value to be changed in
 * {@link #set(Object)}. */
public final class UnmodifiableRef<T> implements Reference<T> {

    public final T obj;

    public UnmodifiableRef(T obj) {
        this.obj = obj;
    }

    @Override
    public T get() {
        return obj;
    }

    @Override
    public boolean set(T value) {
        return false;
    }

    @Override
    public boolean isValid(T value) {
        return false;
    }
}
