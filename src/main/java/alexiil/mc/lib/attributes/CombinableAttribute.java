/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class CombinableAttribute<T> extends DefaultedAttribute<T> {

    public final AttributeCombiner<T> combiner;

    CombinableAttribute(Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner) {
        super(clazz, defaultValue);
        this.combiner = combiner;
    }

    CombinableAttribute(Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner,
        CustomAttributeAdder<T> customAdder) {
        super(clazz, defaultValue, customAdder);
        this.combiner = combiner;
    }

    /** @return Either the {@link DefaultedAttribute #defaultValue defaultValue}, a single instance, or a
     *         {@link #combiner combined} instance depending on how many attribute instances could be found. */
    @Nonnull
    public final T get(World world, BlockPos pos) {
        return get(world, pos, null);
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchOptionDirectional}. A full list of
     *            possible {@link SearchOption}'s is in {@link SearchOptions}.
     * @return Either the {@link DefaultedAttribute #defaultValue defaultValue}, a single instance, or a
     *         {@link #combiner combined} instance depending on how many attribute instances could be found. */
    @Nonnull
    public final T get(World world, BlockPos pos, SearchOption<? super T> searchParam) {
        return getAll(world, pos, searchParam).combine(this);
    }

    /** Shorter method call for the common case of:</br>
     * BlockEntity be = ...;</br>
     * Direction dir = ...;</br>
     * Attribute&lt;T&gt; attr = ...;</br>
     * AttributeList&lt;T&gt; list = attr.{@link #get(World, BlockPos, SearchOption) getAll}(be.getWorld(),
     * be.getPos().offset(dir), {@link SearchOptions#inDirection(Direction) SearchOptions.inDirection}(dir)); </br>
     */
    @Nonnull
    public final T getFromNeighbour(BlockEntity be, Direction dir) {
        return get(be.getWorld(), be.getPos().offset(dir), SearchOptions.inDirection(dir));
    }

    @Nonnull
    public T combine(List<T> list) {
        switch (list.size()) {
            case 0: {
                return defaultValue;
            }
            case 1: {
                return list.get(0);
            }
            default: {
                return combiner.combine(list);
            }
        }
    }

    @Nonnull
    public T combine(List<T> firstList, List<T> secondList) {
        switch (firstList.size() + secondList.size()) {
            case 0: {
                return defaultValue;
            }
            case 1: {
                if (secondList.isEmpty()) {
                    return firstList.get(0);
                } else {
                    return firstList.get(0);
                }
            }
            default: {
                if (firstList.isEmpty()) {
                    return combiner.combine(secondList);
                } else if (secondList.isEmpty()) {
                    return combiner.combine(firstList);
                } else {
                    List<T> combined = new ArrayList<>();
                    combined.add(firstList.get(0));
                    combined.addAll(secondList);
                    return combiner.combine(combined);
                }
            }
        }
    }
}
