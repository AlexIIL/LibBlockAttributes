/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.List;

import javax.annotation.Nonnull;

import alexiil.mc.lib.attributes.misc.Combined;

@FunctionalInterface
public interface AttributeCombiner<T> {

    /** Combines the given list of attributes down into a single one. It is recommended that implementations return
     * instances that implement {@link Combined}. */
    @Nonnull
    T combine(List<? extends T> attributes);
}
