/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;

/** A {@link BlockEntity} that contains attributes.
 * <p>
 * Note that {@link AttributeProvider} is called <em>before</em> this, if the {@link Block} implements it. However if
 * the {@link Block} doesn't actually add any objects of the correct type then this will still be called. */
public interface AttributeProviderBlockEntity {

    /** Adds every instance of the given attribute in this block entity to the resulting list. Note that this must never
     * add wrapped versions of an attribute to the resulting list as the caller is responsible for doing that
     * instead. */
    void addAllAttributes(AttributeList<?> to);
}
