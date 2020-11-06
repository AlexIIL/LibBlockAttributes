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

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.BlockEntityAttributeAdder.BlockEntityAttributeAdderFN;
import alexiil.mc.lib.attributes.CompatLeveledMap.ValueEntry;
import alexiil.mc.lib.attributes.fatjar.FatJarChecker;
import alexiil.mc.lib.attributes.misc.AbstractItemBasedAttribute;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes.LbaModule;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;
import alexiil.mc.lib.attributes.misc.UnmodifiableRef;

/** The central holding class for all attribute instances.
 * <p>
 * An {@link Attribute} can be of a single {@link Class} that should be accessible from blocks or items. Instances can
 * be created from the various static factory methods in {@link Attributes}. Due to the different subclasses no registry
 * is provided, so instances should be stored in a public static final field somewhere near the target class.
 * <p>
 * <h1>Usage</h1> All attributes offer "getAll" and "getFirstOrNull" methods.
 * <p>
 * <h1>Blocks</h1> Instances can be obtained from blocks with the {@link #getAll(World, BlockPos, SearchOption)} or
 * {@link #getFirstOrNull(World, BlockPos, SearchOption)} methods, although the {@link SearchOption} can be omitted (or
 * passed as null) if you don't wish to restrict what attributes are returned to you.<br>
 * For convenience there is also a "getAllFromNeighbour" (and getFirstOrNullFromNeighbour) which takes a
 * {@link BlockEntity} to search from, and a {@link Direction} to search in.
 * <p>
 * <h1>Items</h1> Instances can be obtained from items with the {@link #getAll(Reference, LimitedConsumer, Predicate)}
 * or {@link #getFirstOrNull(Reference, LimitedConsumer, Predicate)} methods, however the predicate may be omitted (or
 * passed as null) if you f you don't wish to restrict what attributes are returned to you.<br>
 * <br>
 * {@link ItemStack}s don't inherently have any information about what they are stored in (unlike blocks) so instead of
 * a world and block position we use a {@link Reference} for the current stack, and a {@link LimitedConsumer} for the
 * excess. The {@link Reference} may contain an item with any count, although generally only the uppermost item on the
 * stack will be used by attributes. Attribute instances which modify the {@link ItemStack} are highly encouraged to
 * extend {@link AbstractItemBasedAttribute} to help manage returning the modified {@link ItemStack} to the reference
 * and limited consumer.
 * <p>
 * <h1>Entities</h1> Currently LBA doesn't offer support for entities, although it is planned.
 * <p>
 * <h1>Subclasses</h1> There are 2 provided subclasses of {@link Attribute}: {@link DefaultedAttribute} and
 * {@link CombinableAttribute}.
 * <p>
 * <h1>Custom Adders</h1> If the target block or item doesn't implement {@link AttributeProvider} or
 * {@link AttributeProviderItem} (or those methods don't offer the attribute instance that you need) then you can create
 * a custom adder for each block or item that you need. The old (deprecated) method of adding custom attribute adders
 * called every single one in the order that they were added. The new method however only matches a single one (per
 * attribute), and has the following order:
 * <ol>
 * <li>If the block or item implements {@link AttributeProvider} or {@link AttributeProviderItem} directly then is is
 * used first. If that adder didn't add anything then the next steps aren't skipped (so it will exit early if the
 * block/item provided any implementations itself, otherwise it will continue to try to find one).</li>
 * <li>If the block state is meant to have a {@link BlockEntity} ({@link BlockState#method_31709()}), and a {@link BlockEntity}
 * is present in the world, and it implements {@link AttributeProviderBlockEntity} then it is checked second. If that
 * adder didn't add anything then the next steps aren't skipped (so it will exit early if the block entity provided any
 * implementations itself, otherwise it will continue to try to find one).</li>
 * <li>{@link AttributeSourceType#INSTANCE} implementations are considered:
 * <ol>
 * <li>If the block/item has an exact mapping registered in
 * {@link #setBlockAdder(AttributeSourceType, Block, CustomAttributeAdder)} then it is used.</li>
 * <li>Next, any predicate adders
 * ({@link #addBlockPredicateAdder(AttributeSourceType, boolean, Predicate, CustomAttributeAdder)}) are considered (if
 * they passed "true" for "specific").</li>
 * <li>The exact class mapped by
 * ({@link #putBlockClassAdder(AttributeSourceType, Class, boolean, CustomAttributeAdder)}) is considered (if they
 * passed "false" for "matchSubclasses").</li>
 * <li>Any super-classes or interfaces mapped by
 * ({@link #putBlockClassAdder(AttributeSourceType, Class, boolean, CustomAttributeAdder)}) is considered (if they
 * passed "true" for "matchSubclasses").</li>
 * <li>Finally, any predicate adders
 * ({@link #addBlockPredicateAdder(AttributeSourceType, boolean, Predicate, CustomAttributeAdder)}) are considered (if
 * they passed "false" for "specific").</li>
 * </ol>
 * </li>
 * <li>{@link AttributeSourceType#COMPAT_WRAPPER} implementations are considered, in the same order as
 * {@link AttributeSourceType#INSTANCE} above.</li>
 * <li>Finally everything registered to {@link #appendBlockAdder(CustomAttributeAdder)} is called. (Unlike every other
 * adder above, every single one is called).</li>
 * </ol>
 */
public class Attribute<T> {
    public final Class<T> clazz;

    private final CompatLeveledMap<Block, Block, CustomAttributeAdder<T>> customBlockMap;
    private final CompatLeveledMap<Item, Item, ItemAttributeAdder<T>> customItemMap;
    private final CompatLeveledMap<BlockEntityType<?>, BlockEntity, BlockEntityAttributeAdder<T, ?>> customBlockEntityMap;
    // FIXME: Add Caching!
    private final ArrayList<CustomAttributeAdder<T>> fallbackBlockAdders = new ArrayList<>();
    private final ArrayList<ItemAttributeAdder<T>> fallbackItemAdders = new ArrayList<>();

    protected Attribute(Class<T> clazz) {
        this.clazz = clazz;
        String name = "attribute " + clazz.getName();
        customBlockMap = new CompatLeveledMap<>(name, Block.class, NullAttributeAdder.get(), Attribute::getName);
        customItemMap = new CompatLeveledMap<>(name, Item.class, NullAttributeAdder.get(), Attribute::getName);
        customBlockEntityMap
            = new CompatLeveledMap<>(name, BlockEntity.class, NullAttributeAdder.get(), Attribute::getName);

        customBlockMap.baseOffset = 0;
        customBlockMap.priorityMultiplier = 2;
        customBlockEntityMap.baseOffset = 1;
        customBlockEntityMap.priorityMultiplier = 2;
    }

    /** @deprecated Kept for backwards compatibility, instead you should call {@link #Attribute(Class)} followed by
     *             {@link #appendBlockAdder(CustomAttributeAdder)}. */
    @Deprecated // (since = "0.5.0", forRemoval = true)
    protected Attribute(Class<T> clazz, CustomAttributeAdder<T> customAdder) {
        this(clazz);
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
    // Custom Adders (Specific)
    //
    // ##########################

    /** Sets the {@link CustomAttributeAdder} for the given block, which is only used if the block in question doesn't
     * implement {@link AttributeProvider}. Only one {@link CustomAttributeAdder} may respond to a singular block. */
    public final void setBlockAdder(AttributeSourceType sourceType, Block block, CustomAttributeAdder<T> adder) {
        customBlockMap.putExact(sourceType, block, adder);
    }

    /** Sets the {@link BlockEntityAttributeAdder} for the given block entity type, which is only used if the block
     * entity in question doesn't implement {@link AttributeProviderBlockEntity}. Only one
     * {@link BlockEntityAttributeAdder} may respond to a singular block. */
    public final <BE extends BlockEntity> void setBlockEntityAdder(
        AttributeSourceType sourceType, BlockEntityType<BE> type, BlockEntityAttributeAdder<T, BE> adder
    ) {
        customBlockEntityMap.putExact(sourceType, type, adder);
    }

    /** Sets the {@link BlockEntityAttributeAdder} for the given block entity type, which is only used if the block
     * entity in question doesn't implement {@link AttributeProviderBlockEntity}. Only one
     * {@link BlockEntityAttributeAdder} may respond to a singular block. */
    public final <BE extends BlockEntity> void setBlockEntityAdder(
        AttributeSourceType sourceType, BlockEntityType<BE> type, Class<BE> clazz,
        BlockEntityAttributeAdderFN<T, BE> adder
    ) {
        BlockEntityAttributeAdder<T, BE> real = BlockEntityAttributeAdder.ofTyped(clazz, adder);
        customBlockEntityMap.putExact(sourceType, type, real);
    }

    /** Sets the {@link BlockEntityAttributeAdder} for the given block entity type, which is only used if the block
     * entity in question doesn't implement {@link AttributeProviderBlockEntity}. Only one
     * {@link BlockEntityAttributeAdder} may respond to a singular block. */
    public final void setBlockEntityAdderFN(
        AttributeSourceType sourceType, BlockEntityType<?> type, BlockEntityAttributeAdderFN<T, BlockEntity> adder
    ) {
        customBlockEntityMap.putExact(sourceType, type, BlockEntityAttributeAdder.ofBasic(adder));
    }

    /** Sets the {@link ItemAttributeAdder} for the given item, which is only used if the item in question doesn't
     * implement {@link AttributeProviderItem}. Only one {@link CustomAttributeAdder} may respond to a singular item. */
    public final void setItemAdder(AttributeSourceType sourceType, Item item, ItemAttributeAdder<T> adder) {
        customItemMap.putExact(sourceType, item, adder);
    }

    /** {@link Predicate}-based block attribute adder. If "specific" is true then these are called directly after
     * {@link #setBlockAdder(AttributeSourceType, Block, CustomAttributeAdder)}, otherwise they are called after the
     * class-based mappings have been called. */
    public final void addBlockPredicateAdder(
        AttributeSourceType sourceType, boolean specific, Predicate<Block> filter, CustomAttributeAdder<T> adder
    ) {
        customBlockMap.addPredicateBased(sourceType, specific, filter, adder);
    }

    /** {@link Predicate}-based block entity attribute adder. If "specific" is true then these are called directly after
     * {@link #setBlockEntityAdder(AttributeSourceType, BlockEntityType, BlockEntityAttributeAdder)}, otherwise they are
     * called after the class-based mappings have been called. */
    public final void addBlockEntityPredicateAdder(
        AttributeSourceType sourceType, boolean specific, Predicate<BlockEntityType<?>> filter,
        BlockEntityAttributeAdderFN<T, BlockEntity> adder
    ) {
        customBlockEntityMap.addPredicateBased(sourceType, specific, filter, BlockEntityAttributeAdder.ofBasic(adder));
    }

    /** {@link Predicate}-based item attribute adder. If "specific" is true then these are called directly after
     * {@link #setItemAdder(AttributeSourceType, Item, ItemAttributeAdder)}, otherwise they are called after the
     * class-based mappings have been called. */
    public final void addItemPredicateAdder(
        AttributeSourceType sourceType, boolean specific, Predicate<Item> filter, ItemAttributeAdder<T> adder
    ) {
        customItemMap.addPredicateBased(sourceType, specific, filter, adder);
    }

    /** {@link Class}-based block attribute adder. If no specific predicate adder has been registered then this checks
     * for an exact class match, and then for a super-type match. Only one adder may be present for any given class. */
    public final void putBlockClassAdder(
        AttributeSourceType sourceType, Class<?> clazz, boolean matchSubclasses, CustomAttributeAdder<T> adder
    ) {
        customBlockMap.putClassBased(sourceType, clazz, matchSubclasses, adder);
    }

    /** {@link Class}-based block entity attribute adder. If no specific predicate adder has been registered then this
     * checks for an exact class match, and then for a super-type match. Only one adder may be present for any given
     * class. */
    public final <BE> void putBlockEntityClassAdder(
        AttributeSourceType sourceType, Class<BE> clazz, boolean matchSubclasses,
        BlockEntityAttributeAdderFN<T, BE> adder
    ) {
        BlockEntityAttributeAdder<T, BE> real = BlockEntityAttributeAdder.ofTyped(clazz, adder);
        customBlockEntityMap.putClassBased(sourceType, clazz, matchSubclasses, real);
    }

    /** {@link Class}-based item attribute adder. If no specific predicate adder has been registered then this checks
     * for an exact class match, and then for a super-type match. */
    public final void putItemClassAdder(
        AttributeSourceType sourceType, Class<?> clazz, boolean matchSubclasses, ItemAttributeAdder<T> adder
    ) {
        customItemMap.putClassBased(sourceType, clazz, matchSubclasses, adder);
    }

    // ##########################
    //
    // Custom Adders (List)
    //
    // ##########################

    /** @deprecated Provided for backwards compatibility - instead you should use
     *             {@link #appendBlockAdder(CustomAttributeAdder)}. */
    @Deprecated // (since = "0.5.0", forRemoval = true)
    public final void appendCustomAdder(CustomAttributeAdder<T> customAdder) {
        appendBlockAdder(customAdder);
    }

    /** Appends a single {@link CustomAttributeAdder} to the list of custom block adders. These are called only for
     * blocks that don't implement {@link AttributeProvider}, or have an existing registration in one of the more
     * specific methods above.
     * 
     * @return This. */
    public Attribute<T> appendBlockAdder(CustomAttributeAdder<T> blockAdder) {
        fallbackBlockAdders.add(blockAdder);
        return this;
    }

    /** Appends a single {@link ItemAttributeAdder} to the list of custom item adders. These are called only for items
     * that don't implement {@link AttributeProviderItem}, or have an existing registration in one of the more specific
     * methods above.
     * 
     * @return This. */
    public Attribute<T> appendItemAdder(ItemAttributeAdder<T> itemAdder) {
        fallbackItemAdders.add(itemAdder);
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
            ((AttributeProvider) block).addAllAttributes(world, pos, state, list);
            if (list.hasOfferedAny()) {
                return;
            }
        }

        BlockEntity be = state.method_31709() ? world.getBlockEntity(pos) : null;
        if (be instanceof AttributeProviderBlockEntity) {
            ((AttributeProviderBlockEntity) be).addAllAttributes(list);
            if (list.hasOfferedAny()) {
                return;
            }
        }

        ValueEntry<CustomAttributeAdder<T>> customBlock = customBlockMap.getEntry(block, block.getClass());
        if (customBlock.priority < 8) {
            customBlock.value.addAll(world, pos, state, list);
            return;
        }

        if (be == null) {
            if (customBlock.priority < CompatLeveledMap.NULL_PRIORITY) {
                customBlock.value.addAll(world, pos, state, list);
                return;
            }
        } else {
            ValueEntry<BlockEntityAttributeAdder<T, ?>> customEntity
                = customBlockEntityMap.getEntry(be.getType(), be.getClass());

            if (customEntity.priority < customBlock.priority) {
                addAll(customEntity.value, be, list);
                return;
            }

            if (customBlock.priority < CompatLeveledMap.NULL_PRIORITY) {
                customBlock.value.addAll(world, pos, state, list);
                return;
            }
        }

        for (CustomAttributeAdder<T> custom : fallbackBlockAdders) {
            custom.addAll(world, pos, state, list);
        }
    }

    private <BE> void addAll(BlockEntityAttributeAdder<T, BE> value, BlockEntity be, AttributeList<T> to) {
        value.addAll(value.getBlockEntityClass().cast(be), to);
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
            int offeredBefore = list.getOfferedCount();
            AttributeProviderItem attributeItem = (AttributeProviderItem) item;
            attributeItem.addAllAttributes(stackRef, excess, list);
            if (offeredBefore < list.getOfferedCount()) {
                return;
            }
        }
        ItemAttributeAdder<T> c = customItemMap.get(item, item.getClass());
        if (c != null) {
            c.addAll(stackRef, excess, list);
            return;
        }
        for (ItemAttributeAdder<T> custom : fallbackItemAdders) {
            custom.addAll(stackRef, excess, list);
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
    public final ItemAttributeList<T> getAll(
        Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess, @Nullable Predicate<T> filter
    ) {

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
    public final T getFirstOrNull(
        Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess, @Nullable Predicate<T> filter
    ) {
        return getAll(stackRef, excess, filter).getFirstOrNull();
    }

    private static String getName(Block block) {
        Identifier id = Registry.BLOCK.getId(block);
        if (!Registry.BLOCK.getDefaultId().equals(id)) {
            return id.toString();
        } else {
            return "UnknownBlock{" + block.getClass() + " @" + Integer.toHexString(System.identityHashCode(block))
                + "}";
        }
    }

    private static String getName(BlockEntityType<?> type) {
        Identifier id = Registry.BLOCK_ENTITY_TYPE.getId(type);
        if (id != null) {
            return id.toString();
        } else {
            return "UnknownBlockEntity{" + type.getClass() + " @" + Integer.toHexString(System.identityHashCode(type))
                + "}";
        }
    }

    private static String getName(Item item) {
        Identifier id = Registry.ITEM.getId(item);
        if (!Registry.ITEM.getDefaultId().equals(id)) {
            return id.toString();
        } else {
            return "UnknownItem{" + item.getClass() + " @" + Integer.toHexString(System.identityHashCode(item)) + "}";
        }
    }

    static {
        validateEnvironment();
    }

    private static void validateEnvironment() throws Error {
        // Environments:
        // 1: self-dev, only "all"
        // 2: self-dev, junit (not loaded by fabric loader)
        // 3: other-dev, only valid subsets
        // 4: other-dev, unit tests (not loaded by fabric loader)
        // 5: other-dev, fatjar (INVALID)
        // 6: other-dev, fatjar + others
        // 7: prod, only valid subsets
        // 8: prod, fatjar (INVALID)
        // 9: prod, fatjar + others (INVALID)

        FabricLoader loader = FabricLoader.getInstance();
        if (loader.getAllMods().isEmpty()) {
            // Must have been loaded by something *other* than fabric itself
            // 2,4
            return;
        }

        ModContainer allModule = LbaModule.ALL.getModContainer();
        ModContainer coreModule = LbaModule.CORE.getModContainer();

        if (coreModule == null) {
            if (allModule == null) {
                // Something else, but still obviously wrong
                throw new Error("(No LBA modules present?)" + FatJarChecker.FATJAR_ERROR);
            } else {
                if ("$version".equals(allModule.getMetadata().getVersion().getFriendlyString())) {
                    // 1
                    return;
                }
                // 5, 8
                throw new Error("(Only 'all' present!)" + FatJarChecker.FATJAR_ERROR);
            }
        }

        if (loader.isDevelopmentEnvironment()) {
            // Anything else is permitted in a dev environment
            // 3, 6
            return;
        }
        // 7
        // 9 (impossible to check here - let items and fluids validate this)
        return;
    }
}
