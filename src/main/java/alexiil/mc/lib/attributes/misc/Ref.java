/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

/** A simple {@link Reference} that holds the value directly in the field {@link #obj}, and accepts any new value. */
public final class Ref<T> implements Reference<T> {
    public T obj;

    public Ref(T obj) {
        this.obj = obj;
    }

    @Override
    public T get() {
        return obj;
    }

    @Override
    public boolean set(T value) {
        obj = value;
        return true;
    }

    @Override
    public boolean isValid(T value) {
        return true;
    }
}
