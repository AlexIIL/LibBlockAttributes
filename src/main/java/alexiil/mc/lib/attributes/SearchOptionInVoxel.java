/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

import net.minecraft.util.shape.VoxelShape;

public final class SearchOptionInVoxel<T> extends SearchOption<T> {
    public final VoxelShape shape;

    SearchOptionInVoxel(VoxelShape shape) {
        this.shape = shape;
    }

    SearchOptionInVoxel(VoxelShape shape, Predicate<T> searchMatcher) {
        super(searchMatcher);
        this.shape = shape;
    }

    @Override
    public VoxelShape getShape() {
        return shape;
    }
}
