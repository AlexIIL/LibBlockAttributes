/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

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

public class DefaultedAttribute<T> extends Attribute<T> {

    /** A non-null default value that can be used as a no-op value if searching failed to find any normal instances. */
    @Nonnull
    public final T defaultValue;

    public DefaultedAttribute(Class<T> clazz, @Nonnull T defaultValue) {
        super(clazz);
        this.defaultValue = defaultValue;
    }

    /** @deprecated Kept for backwards compatibility, instead you should call {@link #DefaultedAttribute(Class, Object)}
     *             followed by {@link #appendBlockAdder(CustomAttributeAdder)}. */
    @Deprecated
    public DefaultedAttribute(Class<T> clazz, @Nonnull T defaultValue, CustomAttributeAdder<T> customAdder) {
        super(clazz, customAdder);
        this.defaultValue = defaultValue;
    }

    @Override
    public DefaultedAttribute<T> appendBlockAdder(CustomAttributeAdder<T> blockAdder) {
        super.appendBlockAdder(blockAdder);
        return this;
    }

    @Override
    public DefaultedAttribute<T> appendItemAdder(ItemAttributeAdder<T> itemAdder) {
        super.appendItemAdder(itemAdder);
        return this;
    }

    // ##########################
    //
    // Block handling
    //
    // ##########################

    /** @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos, SearchOption)}), or the
     *         {@link #defaultValue} if none were found. */
    @Nonnull
    public final T getFirst(World world, BlockPos pos) {
        return getFirst(world, pos, null);
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchOptionDirectional}. A full list of
     *            possible {@link SearchOption}'s is in {@link SearchOptions}.
     * @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos, SearchOption)}), or the
     *         {@link #defaultValue} if the search didn't find any attribute instances at the specified position. */
    @Nonnull
    public final T getFirst(World world, BlockPos pos, SearchOption<? super T> searchParam) {
        return getAll(world, pos, searchParam).getFirst(this);
    }

    /** Shorter method call for the common case of:</br>
     * BlockEntity be = ...;</br>
     * Direction dir = ...;</br>
     * Attribute&lt;T&gt; attr = ...;</br>
     * AttributeList&lt;T&gt; list = attr.{@link #getFirst(World, BlockPos, SearchOption) getAll}(be.getWorld(),
     * be.getPos().offset(dir), {@link SearchOptions#inDirection(Direction) SearchOptions.inDirection}(dir)); </br>
     */
    @Nonnull
    public final T getFirstFromNeighbour(BlockEntity be, Direction dir) {
        return getFirst(be.getWorld(), be.getPos().offset(dir), SearchOptions.inDirection(dir));
    }

    // ##########################
    //
    // Item handling
    //
    // ##########################

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * <p>
     * This method is just a quicker way of calling {@link #getFirst(Reference)} of a single {@link ItemStack} which
     * cannot be modified. Internally this creates a new {@link UnmodifiableRef} for the reference.
     * 
     * @param unmodifiableStack An {@link ItemStack} that may not be modified by any of the attribute instances
     *            returned.
     * @return The first attribute instance found by {@link #getAll(ItemStack)}, or the {@link #defaultValue} if none
     *         were found in the given {@link ItemStack}. */
    @Nonnull
    public final T getFirst(ItemStack unmodifiableStack) {
        return getAll(unmodifiableStack).getFirst(this);
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @return The first attribute instance found by {@link #getAll(Reference)}, or tge {@link #defaultValue} if none
     *         were found in the given {@link ItemStack}. */
    @Nonnull
    public final T getFirst(Reference<ItemStack> stackRef) {
        return getAll(stackRef).getFirst(this);
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * 
     * @param filter A {@link Predicate} to test all {@link ItemAttributeList#add(Object) offered} objects before
     *            accepting them into the list. A null value equals no filter, which will not block any values.
     * @return The first attribute instance found by {@link #getAll(Reference, Predicate)}, or the {@link #defaultValue}
     *         if none were found in the given {@link ItemStack}. */
    @Nonnull
    public final T getFirst(Reference<ItemStack> stackRef, @Nullable Predicate<T> filter) {
        return getAll(stackRef, filter).getFirst(this);
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
     * {@link #defaultValue} if none were found.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @param excess A {@link LimitedConsumer} which allows any of the returned attribute instances to spit out excess
     *            items in addition to changing the main stack. (As this is a LimitedConsumer rather than a normal
     *            consumer it is important to note that excess items items are not guaranteed to be accepted). A null
     *            value will default to {@link LimitedConsumer#rejecting()}.
     * @return The first attribute instance found by {@link #getAll(Reference, LimitedConsumer)}, or the
     *         {@link #defaultValue} if none were found in the given {@link ItemStack}. */
    @Nonnull
    public final T getFirst(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess) {
        return getAll(stackRef, excess).getFirst(this);
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or the
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
     * @return The first attribute instance found by {@link #getAll(Reference, LimitedConsumer, Predicate)}, or the
     *         {@link #defaultValue} if none were found in the given {@link ItemStack}. */
    @Nonnull
    public final T getFirst(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess, @Nullable Predicate<
        T> filter) {
        return getAll(stackRef, excess, filter).getFirst(this);
    }
}
