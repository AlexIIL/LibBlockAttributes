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
import net.minecraft.util.shape.VoxelShapes;

/** The base class for block search options. This only contains filtration for objects that are added to the attribute
 * list via a {@link Predicate}. Instances can be constructed/obtained from {@link SearchOptions}, and all javadoc for
 * the various implementations is on those static factories. */
public class SearchOption<T> {

    /** Use {@link SearchOptions#ALL}. */
    static final SearchOption<Object> ALL = new SearchOption<>();

    private final Predicate<T> searchMatcher;

    SearchOption() {
        this.searchMatcher = null;
    }

    SearchOption(Predicate<T> searchMatcher) {
        this.searchMatcher = searchMatcher;
    }

    /** Checks to see if the given object matches this {@link #searchMatcher}. This method will normally be called by
     * LBA automatically, so users are discouraged from calling this. */
    public final boolean matches(T obj) {
        return searchMatcher != null ? searchMatcher.test(obj) : true;
    }

    /** Returns the {@link VoxelShape} to use for bounds checking. This defaults to a full block, but custom search
     * options (like {@link SearchOptionInVoxel}) override this. */
    public VoxelShape getShape() {
        return VoxelShapes.fullCube();
    }
}
