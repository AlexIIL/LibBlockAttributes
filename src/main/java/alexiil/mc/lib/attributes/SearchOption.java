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

    public final boolean matches(T obj) {
        return searchMatcher != null ? searchMatcher.test(obj) : true;
    }

    /** @return The {@link VoxelShape} to use */
    public VoxelShape getShape() {
        return VoxelShapes.fullCube();
    }
}
