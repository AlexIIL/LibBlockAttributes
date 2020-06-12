/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc.compat.mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import alexiil.mc.lib.attributes.fluid.compat.mod.reborncore.RebornCompatLoader;

public abstract class LbaModCompatLoader {
    protected static Class<?> c(String name) throws ClassNotFoundException {
        return Class.forName(name, false, RebornCompatLoader.class.getClassLoader());
    }

    protected static void requireField(Class<?> cls, String name, Class<?> type) throws ReflectiveOperationException {
        Field f = cls.getField(name);
        if (!type.equals(f.getType())) {
            throw new NoSuchFieldException("Needed the field " + f + " to be of type " + type);
        }
    }

    protected static void requireMethod(Class<?> cls, String name, Class<?>[] args, Class<?> ret)
        throws ReflectiveOperationException {

        Method m = cls.getMethod(name, args);
        if (!ret.equals(m.getReturnType())) {
            throw new NoSuchMethodException("Needed the method " + m + " to return " + ret);
        }
    }

    protected static boolean hasOldMethod(Class<?> cls, String name, Class<?>[] args, Class<?> ret) {
        try {
            Method m = cls.getMethod(name, args);
            return ret.equals(m.getReturnType());
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
