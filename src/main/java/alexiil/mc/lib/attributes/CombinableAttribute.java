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
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;
import alexiil.mc.lib.attributes.misc.UnmodifiableRef;

/** A {@link DefaultedAttribute} that also provides a "get" method to combine every found attribute into a single one,
 * using an {@link AttributeCombiner}. */
public class CombinableAttribute<T> extends DefaultedAttribute<T> {

    private final AttributeCombiner<T> combiner;

    protected CombinableAttribute(Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner) {
        super(clazz, defaultValue);
        this.combiner = combiner;
    }

    @Override
    public CombinableAttribute<T> appendBlockAdder(CustomAttributeAdder<T> blockAdder) {
        super.appendBlockAdder(blockAdder);
        return this;
    }

    @Override
    public CombinableAttribute<T> appendItemAdder(ItemAttributeAdder<T> itemAdder) {
        super.appendItemAdder(itemAdder);
        return this;
    }

    // ##########################
    //
    // Combining
    //
    // ##########################

    @Nonnull
    public final T combine(List<T> list) {
        switch (list.size()) {
            case 0: {
                return defaultValue;
            }
            case 1: {
                return asNonNull(list.get(0));
            }
            default: {
                return combiner.combine(list);
            }
        }
    }

    @Nonnull
    public final T combine(List<T> firstList, List<T> secondList) {
        switch (firstList.size() + secondList.size()) {
            case 0: {
                return defaultValue;
            }
            case 1: {
                if (secondList.isEmpty()) {
                    return asNonNull(firstList.get(0));
                } else {
                    return asNonNull(firstList.get(0));
                }
            }
            default: {
                if (firstList.isEmpty()) {
                    return combiner.combine(secondList);
                } else if (secondList.isEmpty()) {
                    return combiner.combine(firstList);
                } else {
                    List<T> combined = new ArrayList<>();
                    combined.add(asNonNull(firstList.get(0)));
                    combined.addAll(secondList);
                    return combiner.combine(combined);
                }
            }
        }
    }

    @Nonnull
    private final T asNonNull(@Nullable T value) {
        if (value == null) {
            throw new NullPointerException("The value was null, when all elements are meant to be non-null!");
        }
        return value;
    }

    // ##########################
    //
    // Block handling
    //
    // ##########################

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

    // ##########################
    //
    // Item handling
    //
    // ##########################

    /** Obtains a combined instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * <p>
     * This method is just a quicker way of calling {@link #getFirst(Reference)} of a single {@link ItemStack} which
     * cannot be modified. Internally this creates a new {@link UnmodifiableRef} for the reference.
     * 
     * @param unmodifiableStack An {@link ItemStack} that may not be modified by any of the attribute instances
     *            returned.
     * @return The combined attribute instance found by {@link #getAll(ItemStack)}, or the {@link #defaultValue} if none
     *         were found in the given {@link ItemStack}. */
    @Nonnull
    public final T get(ItemStack unmodifiableStack) {
        return getAll(unmodifiableStack).combine(this);
    }

    /** Obtains a combined instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @return The combined attribute instance found by {@link #getAll(Reference)}, or tge {@link #defaultValue} if none
     *         were found in the given {@link ItemStack}. */
    @Nonnull
    public final T get(Reference<ItemStack> stackRef) {
        return getAll(stackRef).combine(this);
    }

    /** Obtains a combined instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * 
     * @param filter A {@link Predicate} to test all {@link ItemAttributeList#add(Object) offered} objects before
     *            accepting them into the list. A null value equals no filter, which will not block any values.
     * @return The combined attribute instance found by {@link #getAll(Reference, Predicate)}, or the
     *         {@link #defaultValue} if none were found in the given {@link ItemStack}. */
    @Nonnull
    public final T get(Reference<ItemStack> stackRef, @Nullable Predicate<T> filter) {
        return getAll(stackRef, filter).combine(this);
    }

    /** Obtains a combined instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @param excess A {@link LimitedConsumer} which allows any of the returned attribute instances to spit out excess
     *            items in addition to changing the main stack. (As this is a LimitedConsumer rather than a normal
     *            consumer it is important to note that excess items items are not guaranteed to be accepted). A null
     *            value will default to {@link LimitedConsumer#rejecting()}.
     * @return The combined attribute instance found by {@link #getAll(Reference, LimitedConsumer)}, or the
     *         {@link #defaultValue} if none were found in the given {@link ItemStack}. */
    @Nonnull
    public final T get(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess) {
        return getAll(stackRef, excess).combine(this);
    }

    /** Obtains a combined instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @param excess A {@link LimitedConsumer} which allows any of the returned attribute instances to spit out excess
     *            items in addition to changing the main stack. (As this is a LimitedConsumer rather than a normal
     *            consumer it is important to note that excess items items are not guaranteed to be accepted). A null
     *            value will default to {@link LimitedConsumer#rejecting()}.
     * @param filter A {@link Predicate} to test all {@link ItemAttributeList#add(Object) offered} objects before
     *            accepting them into the list. A null value equals no filter, which will not block any values.
     * @return The combined attribute instance found by {@link #getAll(Reference, LimitedConsumer, Predicate)}, or the
     *         {@link #defaultValue} if none were found in the given {@link ItemStack}. */
    @Nonnull
    public final T get(
        Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess, @Nullable Predicate<T> filter
    ) {
        return getAll(stackRef, excess, filter).combine(this);
    }
}
