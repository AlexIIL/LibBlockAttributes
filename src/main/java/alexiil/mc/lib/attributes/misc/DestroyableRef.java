/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

/** A {@link Reference} that can be modified until {@link #destroy()} is called, after which all calls to
 * {@link #set(Object)} and {@link #isValid(Object)} will return false. */
public final class DestroyableRef<T> implements Reference<T> {

    private final Reference<T> ref;
    private boolean isAlive = true;

    public DestroyableRef(Reference<T> ref) {
        this.ref = ref;
    }

    public void destroy() {
        isAlive = false;
    }

    @Override
    public T get() {
        return ref.get();
    }

    @Override
    public boolean set(T value) {
        return isAlive && ref.set(value);
    }

    @Override
    public boolean isValid(T value) {
        return isAlive && ref.isValid(value);
    }

}
