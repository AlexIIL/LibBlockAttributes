/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.EnumMap;
import java.util.function.Predicate;

import net.minecraft.util.math.Direction;

public class SearchOptionDirectional<T> extends SearchOption<T> {

    private static final EnumMap<Direction, SearchOptionDirectional<Object>> SIDES;

    /** The direction that this search is going in. */
    public final Direction direction;

    SearchOptionDirectional(Direction direction) {
        this.direction = direction;
    }

    SearchOptionDirectional(Direction direction, Predicate<T> searchMatcher) {
        super(searchMatcher);
        this.direction = direction;
    }

    static SearchOptionDirectional<Object> of(Direction dir) {
        return SIDES.get(dir);
    }

    static {
        SIDES = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            SIDES.put(dir, new SearchOptionDirectional<>(dir));
        }
    }
}
