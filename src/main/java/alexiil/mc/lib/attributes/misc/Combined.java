/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import alexiil.mc.lib.attributes.AttributeCombiner;

/** An {@link Object} that combines several sub objects of one type into a single object of the same type. This is
 * generally the inverse of {@link AttributeCombiner#combine(java.util.List)}. */
public interface Combined {

    /** @return The number of sub-objects that this combines to make. May return -1 if this cannot easily expose the
     *         sub-objects cleanly. */
    int getSubObjectCount();

    /** @param index The index, between 0 and {@link #getSubObjectCount()}.
     * @throws RuntimeException if the index is &lt;0 or &gt;={@link #getSubObjectCount()}. */
    Object getSubObject(int index);
}
