/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import javax.annotation.Nullable;

/** Simple interface for converting this object into another {@link Class}. It is always a good idea to check to see if
 * this object is an instance of the desired class before calling {@link #convertTo(Class)}, because it is not required
 * that it return itself. */
public interface Convertible {

    /** Attempts to provide a variant of this in the given class form. This does not have to return "this" object if
     * this is already an instance of the given class.
     * <p>
     * 
     * @apiNote In order to be typesafe (and prevent crashes) it is recommended that you return
     *          {@link Class#cast(Object)} with the object you wish to return rather than just blindly cast to "T". */
    @Nullable
    <T> T convertTo(Class<T> otherType);

    /** A helper method for {@link #convertTo(Class)} to quickly write implementations that only return a single other
     * object */
    @Nullable
    public static <T> T offer(Class<T> clazz, Object obj) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        return null;
    }

    /** A helper method for {@link #convertTo(Class)} to quickly write implementations that return any of a few
     * predefined objects. */
    @Nullable
    public static <T> T offer(Class<T> clazz, Object... objects) {
        for (Object obj : objects) {
            if (clazz.isInstance(obj)) {
                return clazz.cast(obj);
            }
        }
        return null;
    }
}
