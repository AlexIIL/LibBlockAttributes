/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.util.ArrayList;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;
import alexiil.mc.lib.attributes.misc.UnmodifiableRef;

public class Attribute<T> {
    public final Class<T> clazz;

    private final ArrayList<CustomAttributeAdder<T>> blockAdders = new ArrayList<>();
    private final ArrayList<ItemAttributeAdder<T>> itemAdders = new ArrayList<>();

    protected Attribute(Class<T> clazz) {
        this.clazz = clazz;
    }

    /** @deprecated Kept for backwards compatibility, instead you should call {@link #Attribute(Class)} followed by
     *             {@link #appendBlockAdder(CustomAttributeAdder)}. */
    @Deprecated
    protected Attribute(Class<T> clazz, CustomAttributeAdder<T> customAdder) {
        this.clazz = clazz;
        appendBlockAdder(customAdder);
    }

    /** Checks to see if the given object is an {@link Class#isInstance(Object)} of this attribute. */
    public final boolean isInstance(Object obj) {
        return clazz.isInstance(obj);
    }

    /** {@link Class#cast(Object) Casts} The given object to type of this attribute. */
    public final T cast(Object obj) {
        return clazz.cast(obj);
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    // ##########################
    //
    // Custom Adders
    //
    // ##########################

    /** @deprecated Provided for backwards compatibility - instead you should use
     *             {@link #appendBlockAdder(CustomAttributeAdder)}. */
    @Deprecated
    public final void appendCustomAdder(CustomAttributeAdder<T> customAdder) {
        appendBlockAdder(customAdder);
    }

    /** Appends a single {@link CustomAttributeAdder} to the list of custom block adders. These are called only for
     * blocks that don't implement {@link AttributeProvider}.
     * 
     * @return This. */
    public Attribute<T> appendBlockAdder(CustomAttributeAdder<T> blockAdder) {
        blockAdders.add(blockAdder);
        return this;
    }

    /** Appends a single {@link ItemAttributeAdder} to the list of custom item adders. These are called only for items
     * that don't implement {@link AttributeProviderItem}.
     * 
     * @return This. */
    public Attribute<T> appendItemAdder(ItemAttributeAdder<T> itemAdder) {
        itemAdders.add(itemAdder);
        return this;
    }

    // ##########################
    //
    // Block handling
    //
    // ##########################

    final void addAll(World world, BlockPos pos, AttributeList<T> list) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof AttributeProvider) {
            AttributeProvider attributeBlock = (AttributeProvider) block;
            attributeBlock.addAllAttributes(world, pos, state, list);
        } else {
            for (CustomAttributeAdder<T> custom : blockAdders) {
                custom.addAll(world, pos, state, list);
            }
        }
    }

    /** @return A complete {@link AttributeList} of every attribute instance that can be found. */
    public final AttributeList<T> getAll(World world, BlockPos pos) {
        return getAll(world, pos, null);
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchOptionDirectional}. A full list of
     *            possible {@link SearchOption}'s is in {@link SearchOptions}.
     * @return A complete {@link AttributeList} of every attribute instance that can be found with the supplied search
     *         parameters. */
    public final AttributeList<T> getAll(World world, BlockPos pos, SearchOption<? super T> searchParam) {
        VoxelShape blockShape = world.getBlockState(pos).getOutlineShape(world, pos);
        AttributeList<T> list = new AttributeList<>(this, searchParam, blockShape);
        addAll(world, pos, list);
        list.finishAdding();
        return list;
    }

    /** Shorter method call for the common case of:</br>
     * BlockEntity be = ...;</br>
     * Direction dir = ...;</br>
     * Attribute&lt;T&gt; attr = ...;</br>
     * AttributeList&lt;T&gt; list = attr.{@link #getAll(World, BlockPos, SearchOption) getAll}(be.getWorld(),
     * be.getPos().offset(dir), {@link SearchOptions#inDirection(Direction) SearchOptions.inDirection}(dir)); </br>
     */
    public final AttributeList<T> getAllFromNeighbour(BlockEntity be, Direction dir) {
        return getAll(be.getWorld(), be.getPos().offset(dir), SearchOptions.inDirection(dir));
    }

    /** @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos)}), or null if this didn't
     *         find any instances. */
    @Nullable
    public final T getFirstOrNull(World world, BlockPos pos) {
        return getFirstOrNull(world, pos, null);
    }

    /** @param searchParam The search parameters to use for accessing instances. Many blocks only offer attributes from
     *            a certain direction, which should be provided as a {@link SearchOptionDirectional}. A full list of
     *            possible {@link SearchOption}'s is in {@link SearchOptions}.
     * @return The first attribute instance (as obtained by {@link #getAll(World, BlockPos, SearchOption)}), or null if
     *         the search didn't find any attribute instances at the specified position. */
    @Nullable
    public final T getFirstOrNull(World world, BlockPos pos, @Nullable SearchOption<? super T> searchParam) {
        return getAll(world, pos, searchParam).getFirstOrNull();
    }

    /** Shorter method call for the common case of:</br>
     * BlockEntity be = ...;</br>
     * Direction dir = ...;</br>
     * Attribute&lt;T&gt; attr = ...;</br>
     * AttributeList&lt;T&gt; list = attr.{@link #getFirstOrNull(World, BlockPos, SearchOption) getAll}(be.getWorld(),
     * be.getPos().offset(dir), {@link SearchOptions#inDirection(Direction) SearchOptions.inDirection}(dir)); </br>
     */
    @Nullable
    public final T getFirstOrNullFromNeighbour(BlockEntity be, Direction dir) {
        return getFirstOrNull(be.getWorld(), be.getPos().offset(dir), SearchOptions.inDirection(dir));
    }

    // ##########################
    //
    // ItemStack handling
    //
    // ##########################

    final void addAll(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess, ItemAttributeList<T> list) {
        ItemStack stack = stackRef.get();
        Item item = stack.getItem();

        if (item instanceof AttributeProviderItem) {
            AttributeProviderItem attributeItem = (AttributeProviderItem) item;
            attributeItem.addAllAttributes(stackRef, excess, list);
        } else {
            for (ItemAttributeAdder<T> custom : itemAdders) {
                custom.addAll(stackRef, excess, list);
            }
        }
    }

    /** Obtains all instances of this attribute in the given {@link ItemStack} {@link Reference}.
     * <p>
     * This method is just a quicker way of calling {@link #getAll(Reference)} of a single {@link ItemStack} which
     * cannot be modified. Internally this creates a new {@link UnmodifiableRef} for the reference.
     * 
     * @param unmodifiableStack An {@link ItemStack} that may not be modified by any of the attribute instances
     *            returned.
     * @return A complete {@link AttributeList} of every attribute instance that can be found in the given
     *         {@link ItemStack}. */
    public final ItemAttributeList<T> getAll(ItemStack unmodifiableStack) {
        return getAll(new UnmodifiableRef<>(unmodifiableStack), null, null);
    }

    /** Obtains all instances of this attribute in the given {@link ItemStack} {@link Reference}.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @return A complete {@link AttributeList} of every attribute instance that can be found in the given
     *         {@link ItemStack}. */
    public final ItemAttributeList<T> getAll(Reference<ItemStack> stackRef) {
        return getAll(stackRef, LimitedConsumer.rejecting(), null);
    }

    /** Obtains all instances of this attribute in the given {@link ItemStack} {@link Reference}.
     * 
     * @param filter A {@link Predicate} to test all {@link ItemAttributeList#add(Object) offered} objects before
     *            accepting them into the list. A null value equals no filter, which will not block any values.
     * @return A complete {@link AttributeList} of every attribute instance that can be found in the given
     *         {@link ItemStack}. */
    public final ItemAttributeList<T> getAll(Reference<ItemStack> stackRef, @Nullable Predicate<T> filter) {
        return getAll(stackRef, LimitedConsumer.rejecting(), filter);
    }

    /** Obtains all instances of this attribute in the given {@link ItemStack} {@link Reference}.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @param excess A {@link LimitedConsumer} which allows any of the returned attribute instances to spit out excess
     *            items in addition to changing the main stack. (As this is a LimitedConsumer rather than a normal
     *            consumer it is important to note that excess items items are not guaranteed to be accepted). A null
     *            value will default to {@link LimitedConsumer#rejecting()}.
     * @return A complete {@link AttributeList} of every attribute instance that can be found in the given
     *         {@link ItemStack}. */
    public final ItemAttributeList<T> getAll(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess) {
        return getAll(stackRef, excess, null);
    }

    /** Obtains all instances of this attribute in the given {@link ItemStack} {@link Reference}.
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
     * @return A complete {@link AttributeList} of every attribute instance that can be found in the given
     *         {@link ItemStack}. */
    public final ItemAttributeList<T> getAll(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess,
        @Nullable Predicate<T> filter) {

        if (excess == null) {
            excess = LimitedConsumer.rejecting();
        }

        ItemAttributeList<T> list = new ItemAttributeList<>(this, filter);
        addAll(stackRef, excess, list);
        list.finishAdding();
        return list;
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or null if none
     * were found.
     * <p>
     * This method is just a quicker way of calling {@link #getAll(Reference)} of a single {@link ItemStack} which
     * cannot be modified. Internally this creates a new {@link UnmodifiableRef} for the reference.
     * 
     * @param unmodifiableStack An {@link ItemStack} that may not be modified by any of the attribute instances
     *            returned.
     * @return The first attribute instance found by {@link #getAll(ItemStack)}, or null if none were found in the given
     *         {@link ItemStack}. */
    @Nullable
    public final T getFirstOrNull(ItemStack unmodifiableStack) {
        return getAll(unmodifiableStack).getFirstOrNull();
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or null if none
     * were found.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @return The first attribute instance found by {@link #getAll(Reference)}, or null if none were found in the given
     *         {@link ItemStack}. */
    @Nullable
    public final T getFirstOrNull(Reference<ItemStack> stackRef) {
        return getAll(stackRef).getFirstOrNull();
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or null if none
     * were found.
     * 
     * @param filter A {@link Predicate} to test all {@link ItemAttributeList#add(Object) offered} objects before
     *            accepting them into the list. A null value equals no filter, which will not block any values.
     * @return The first attribute instance found by {@link #getAll(Reference, Predicate)}, or null if none were found
     *         in the given {@link ItemStack}. */
    @Nullable
    public final T getFirstOrNull(Reference<ItemStack> stackRef, @Nullable Predicate<T> filter) {
        return getAll(stackRef, filter).getFirstOrNull();
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or null if none
     * were found.
     * 
     * @param stackRef A {@link Reference} to the {@link ItemStack} to be searched. This is a full reference, which may
     *            allow any of the returned attribute instances to modify it. (For example if it was in an inventory
     *            then changes would be correctly reflected in the backing inventory).
     * @param excess A {@link LimitedConsumer} which allows any of the returned attribute instances to spit out excess
     *            items in addition to changing the main stack. (As this is a LimitedConsumer rather than a normal
     *            consumer it is important to note that excess items items are not guaranteed to be accepted). A null
     *            value will default to {@link LimitedConsumer#rejecting()}.
     * @return The first attribute instance found by {@link #getAll(Reference, LimitedConsumer)}, or null if none were
     *         found in the given {@link ItemStack}. */
    @Nullable
    public final T getFirstOrNull(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess) {
        return getAll(stackRef, excess).getFirstOrNull();
    }

    /** Obtains the first instance of this attribute in the given {@link ItemStack} {@link Reference}, or null if none
     * were found.
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
     * @return The first attribute instance found by {@link #getAll(Reference, LimitedConsumer, Predicate)}, or null if
     *         none were found in the given {@link ItemStack}. */
    @Nullable
    public final T getFirstOrNull(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess, @Nullable Predicate<
        T> filter) {
        return getAll(stackRef, excess, filter).getFirstOrNull();
    }
}
