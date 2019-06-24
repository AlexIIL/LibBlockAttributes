/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.function.Predicate;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public final class SearchOptionDirectionalVoxel<T> extends SearchOptionDirectional<T> {

    /** If true then the {@link AttributeList} will sort itself based on the direction and shape. */
    public final boolean ordered;
    public final VoxelShape shape;

    SearchOptionDirectionalVoxel(Direction direction, boolean ordered, VoxelShape shape) {
        super(direction);
        this.ordered = ordered;
        this.shape = shape;
    }

    SearchOptionDirectionalVoxel(Direction direction, boolean ordered, VoxelShape shape, Predicate<T> searchMatcher) {
        super(direction, searchMatcher);
        this.ordered = ordered;
        this.shape = shape;
    }

    @Override
    public VoxelShape getShape() {
        return shape;
    }
}
