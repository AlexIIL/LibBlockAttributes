/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A {@link Block} that contains attributes. */
public interface AttributeProvider {

    /** Adds every instance of the given attribute in this block to the resulting list. Note that this must never add
     * wrapped versions of an attribute to the resulting list as the caller is responsible for doing that instead. */
    void addAllAttributes(World world, BlockPos pos, BlockState state, AttributeList<?> to);
}
