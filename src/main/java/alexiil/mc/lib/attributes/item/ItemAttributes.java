/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.math.Fraction;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.AttributeCombiner;
import alexiil.mc.lib.attributes.AttributeSourceType;
import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fatjar.FatJarChecker;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.item.FixedItemInv.CopyingFixedItemInv;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.compat.FixedSidedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.compat.mod.LbaItemModCompat;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInvView;
import alexiil.mc.lib.attributes.item.impl.CombinedGroupedItemInv;
import alexiil.mc.lib.attributes.item.impl.CombinedGroupedItemInvView;
import alexiil.mc.lib.attributes.item.impl.CombinedItemExtractable;
import alexiil.mc.lib.attributes.item.impl.CombinedItemInsertable;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyGroupedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes.LbaModule;
import alexiil.mc.lib.attributes.misc.Reference;

public final class ItemAttributes {
    private ItemAttributes() {}

    public static final CombinableAttribute<FixedItemInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<FixedItemInv> FIXED_INV;
    public static final CombinableAttribute<GroupedItemInvView> GROUPED_INV_VIEW;
    public static final CombinableAttribute<GroupedItemInv> GROUPED_INV;
    public static final CombinableAttribute<ItemInsertable> INSERTABLE;
    public static final CombinableAttribute<ItemExtractable> EXTRACTABLE;
    // TODO: Add MultiAttribute<ItemTransferable> to be able to get *everything* in a single call

    /** Mostly intended to be used for {@link ItemStack}'s, not {@link Block}'s. (As this interface doesn't really make
     * much sense when applied to block's alone, however it makes much more sense in pipe input or extraction
     * filters). */
    public static final CombinableAttribute<ItemFilter> FILTER;

    /** A {@link List} of every inventory-type attribute, so: {@link #FIXED_INV_VIEW}, {@link #FIXED_INV},
     * {@link #GROUPED_INV_VIEW}, {@link #GROUPED_INV}, {@link #INSERTABLE}, and {@link #EXTRACTABLE}. */
    public static final List<CombinableAttribute<?>> INVENTORY_BASED;

    /** A {@link List} of every inventory-type attribute, so: {@link #GROUPED_INV_VIEW}, {@link #GROUPED_INV},
     * {@link #INSERTABLE}, and {@link #EXTRACTABLE}. */
    public static final List<CombinableAttribute<?>> GROUPED_INVENTORY_BASED;

    /** Runs the given {@link Consumer} on every {@link #INVENTORY_BASED} attribute. */
    public static void forEachInv(Consumer<? super CombinableAttribute<?>> consumer) {
        INVENTORY_BASED.forEach(consumer);
    }

    /** Runs the given {@link Consumer} on every {@link #GROUPED_INVENTORY_BASED} attribute. */
    public static void forEachGroupedInv(Consumer<? super CombinableAttribute<?>> consumer) {
        GROUPED_INVENTORY_BASED.forEach(consumer);
    }

    static {
        FIXED_INV_VIEW = createFixed(
            FixedItemInvView.class, //
            EmptyFixedItemInv.INSTANCE, //
            list -> new CombinedFixedItemInvView<>(list), //
            inv -> inv//
        );
        FIXED_INV = createFixed(
            FixedItemInv.class, //
            EmptyFixedItemInv.INSTANCE, //
            list -> new CombinedFixedItemInv<>(list), //
            Function.identity()//
        );
        GROUPED_INV_VIEW = create(
            GroupedItemInvView.class, //
            EmptyGroupedItemInv.INSTANCE, //
            list -> new CombinedGroupedItemInvView(list), //
            FixedItemInv::getGroupedInv,//
            inv -> inv//
        );
        GROUPED_INV = create(
            GroupedItemInv.class, //
            EmptyGroupedItemInv.INSTANCE, //
            list -> new CombinedGroupedItemInv(list), //
            FixedItemInv::getGroupedInv,//
            Function.identity()//
        );
        INSERTABLE = create(
            ItemInsertable.class, //
            RejectingItemInsertable.NULL, //
            list -> new CombinedItemInsertable(list), //
            FixedItemInv::getInsertable,//
            inv -> inv//
        );
        EXTRACTABLE = create(
            ItemExtractable.class, //
            EmptyItemExtractable.NULL, //
            list -> new CombinedItemExtractable(list), //
            FixedItemInv::getExtractable,//
            inv -> inv//
        );
        FILTER = Attributes.createCombinable(
            ItemFilter.class, //
            ConstantItemFilter.NOTHING, //
            list -> AggregateItemFilter.allOf(list)//
        );

        INVENTORY_BASED = Arrays.asList(
            FIXED_INV_VIEW, FIXED_INV, //
            GROUPED_INV_VIEW, GROUPED_INV, //
            INSERTABLE, EXTRACTABLE//
        );
        GROUPED_INVENTORY_BASED = Arrays.asList(
            GROUPED_INV_VIEW, GROUPED_INV, //
            INSERTABLE, EXTRACTABLE//
        );

        LbaItemModCompat.load();
    }

    private static <T> CombinableAttribute<T> createFixed(
        Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner, Function<FixedItemInv, T> convertor
    ) {
        CombinableAttribute<T> attribute = Attributes.createCombinable(clazz, defaultValue, combiner);

        AttributeSourceType srcType = AttributeSourceType.COMPAT_WRAPPER;
        addInventoryProvider(convertor, attribute, srcType);

        addChestBlock(convertor, attribute, srcType);

        addInventoryBlockEntities(convertor, attribute);

        addShulkerBox(convertor, attribute, srcType);

        return attribute;
    }
    
    private static <T> CombinableAttribute<T> create(Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner, Function<FixedItemInv, T> fixedConvertor, Function<GroupedItemInv, T> groupedConverter) {
        CombinableAttribute<T> attribute = Attributes.createCombinable(clazz, defaultValue, combiner);

        AttributeSourceType srcType = AttributeSourceType.COMPAT_WRAPPER;
        addInventoryProvider(fixedConvertor, attribute, srcType);

        addChestBlock(fixedConvertor, attribute, srcType);

        addInventoryBlockEntities(fixedConvertor, attribute);

        addShulkerBox(fixedConvertor, attribute, srcType);
        
        addBundle(groupedConverter, attribute, srcType);
        
        return attribute;
    }

    private static <T> void addInventoryProvider(Function<FixedItemInv, T> convertor, CombinableAttribute<T> attribute, AttributeSourceType srcType) {
        attribute.putBlockClassAdder(srcType, InventoryProvider.class, true, (w, p, s, l) -> {
            InventoryProvider provider = (InventoryProvider) s.getBlock();
            SidedInventory inventory = provider.getInventory(s, w, p);
            if (inventory != null) {
                if (inventory.size() > 0) {
                    Direction direction = l.getSearchDirection();
                    Direction blockSide = direction == null ? null : direction.getOpposite();

                    final FixedItemInv wrapper;
                    if (direction != null) {
                        wrapper = FixedSidedInventoryVanillaWrapper.create(inventory, blockSide);
                    } else {
                        wrapper = new FixedInventoryVanillaWrapper(inventory);
                    }
                    l.add(convertor.apply(wrapper));
                } else {
                    l.add(attribute.defaultValue);
                }
            }
        });
    }

    private static <T> void addChestBlock(Function<FixedItemInv, T> convertor, CombinableAttribute<T> attribute, AttributeSourceType srcType) {
        attribute.putBlockClassAdder(srcType, ChestBlock.class, true, (w, p, s, l) -> {
            boolean checkForBlockingCats = false;
            ChestBlock chest = (ChestBlock) s.getBlock();
            Inventory chestInv = ChestBlock.getInventory(chest, s, w, p, checkForBlockingCats);
            if (chestInv != null) {
                l.add(convertor.apply(new FixedInventoryVanillaWrapper(chestInv)));
            }
        });
    }

    private static <T> void addInventoryBlockEntities(Function<FixedItemInv, T> convertor, CombinableAttribute<T> attribute) {
        attribute.appendBlockAdder((w, p, s, l) -> {
            if (!s.hasBlockEntity()) {
                return;
            }
            Direction direction = l.getSearchDirection();
            Direction blockSide = direction == null ? null : direction.getOpposite();
            BlockEntity be = w.getBlockEntity(p);

            if (be instanceof SidedInventory) {
                SidedInventory sidedInv = (SidedInventory) be;
                final FixedItemInv wrapper;
                if (direction != null) {
                    wrapper = FixedSidedInventoryVanillaWrapper.create(sidedInv, blockSide);
                } else {
                    wrapper = new FixedInventoryVanillaWrapper(sidedInv);
                }
                l.add(convertor.apply(wrapper));
            } else if (be instanceof Inventory) {
                l.add(convertor.apply(new FixedInventoryVanillaWrapper((Inventory) be)));
            }
        });
    }

    private static <T> void addShulkerBox(Function<FixedItemInv, T> convertor, CombinableAttribute<T> attribute, AttributeSourceType srcType) {
        attribute.addItemPredicateAdder(srcType, true, ItemAttributes::isShulkerBox, (ref, excess, list) -> {
            list.add(convertor.apply(new ShulkerBoxItemInv(ref)));
        });
    }

    private static <T> void addBundle(Function<GroupedItemInv,T> converter, CombinableAttribute<T> attribute, AttributeSourceType srcType) {
        attribute.addItemPredicateAdder(srcType, true, ItemAttributes::isBundle, (ref, excess, list) -> {
            list.add(converter.apply(new BundleItemInv(ref)));
        });
    }

    static boolean isShulkerBox(Item item) {
        return Block.getBlockFromItem(item) instanceof ShulkerBoxBlock;
    }
    
    static boolean isBundle(Item item) {
        return item instanceof BundleItem;
    }

    abstract static class AbstractFixedItemInv implements CopyingFixedItemInv {
        protected final Reference<ItemStack> ref;

        public AbstractFixedItemInv(Reference<ItemStack> ref) {
            this.ref = ref;
        }

        public abstract int getSlotCount();

        public ItemStack getInvStack(int slot) {
            assert 0 <= slot && slot < 27;

            ItemStack stack = ref.get();
            ContainerComponent component = stack.get(DataComponentTypes.CONTAINER);
            if (component == null || stack.isEmpty() || stack.getCount() != 1 || !ItemAttributes.isShulkerBox(stack.getItem())) {
                return ItemStack.EMPTY;
            }

            DefaultedList<ItemStack> list = DefaultedList.ofSize(getSlotCount(), ItemStack.EMPTY);
            component.copyTo(list);
            if (slot >= list.size()) {
                return ItemStack.EMPTY;
            }
            return list.get(slot);
        }

        public ItemStack getUnmodifiableInvStack(int slot) {
            // Because we deserialise every time it's safe to just return it
            return getInvStack(slot);
        }

        public abstract boolean isItemValidForSlot(int slot, ItemStack stack);

        public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
            if (slot <= 0 || slot > 27) {
                return false;
            }

            if (!isItemValidForSlot(slot, to)) {
                return false;
            }

            ItemStack stack = ref.get();
            if (!stack.isEmpty() || stack.getCount() != 1 || !ItemAttributes.isShulkerBox(stack.getItem())) {
                return false;
            }

            if (simulation == Simulation.ACTION) {
                stack = stack.copy();
            }

            ContainerComponent component = stack.get(DataComponentTypes.CONTAINER);
            if (component == null) {
                component = ContainerComponent.DEFAULT;
            }

            DefaultedList<ItemStack> list = DefaultedList.ofSize(getSlotCount(), ItemStack.EMPTY);
            component.copyTo(list);

            list.set(slot, to);

            if (simulation.isAction()) {
                stack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(list));
            }
            return ref.set(stack, simulation);
        }
    }

    static final class ShulkerBoxItemInv extends AbstractFixedItemInv implements CopyingFixedItemInv {

        private ShulkerBoxItemInv(Reference<ItemStack> ref) {
            super(ref);
        }

        @Override
        public int getSlotCount() {
            return 27;
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            // Check for grouped item inv because everything else boils down to this
            // (Plus we don't care about insertable or extractable's, only inventories)
            return stack.isEmpty() || ItemAttributes.GROUPED_INV_VIEW.getFirstOrNull(stack) == null;
        }

    }
    
    static final class BundleItemInv implements GroupedItemInv {
        private static final Fraction NESTED_BUNDLE_OCCUPANCY = Fraction.getFraction(1, 16);

        final Reference<ItemStack> ref;

        BundleItemInv(Reference<ItemStack> ref) {
            this.ref = ref;
        }

        @Override
        public Set<ItemStack> getStoredStacks() {
            ItemStack stack = ref.get();
            BundleContentsComponent component = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (component == null) {
                return Set.of();
            }
            return ImmutableSet.copyOf(component.iterateCopy());
        }

        @Override
        public int getTotalCapacity() {
            return 64;
        }

        @Override
        public ItemInvStatistic getStatistics(ItemFilter filter) {
            int amount = 0;
            Fraction filterOccupancy = Fraction.ZERO;
            for (ItemStack stack : getStacks()) {
                if (filter.matches(stack)) {
                    amount += stack.getCount();
                    if (Objects.equals(filterOccupancy, Fraction.ZERO)) {
                        filterOccupancy = getOccupancy(stack);
                    } else {
                        filterOccupancy = null;
                    }
                }
            }

            int space;
            if (filterOccupancy != null && !filterOccupancy.equals(Fraction.ZERO)) {
                Fraction spaceFrac = Fraction.ONE.subtract(getOccupancy());
                space = Math.max(spaceFrac.divideBy(filterOccupancy).intValue(), 0);
            } else {
                space = 0;
            }

            return new ItemInvStatistic(filter, amount, space, -1);
        }

        @Override
        public int getCapacity(ItemStack stack) {
            Fraction occupancy = Fraction.ZERO;
            for (ItemStack oldStack : getStacks()) {
                if (!ItemStackUtil.areEqualIgnoreAmounts(stack, oldStack)) {
                    occupancy = occupancy.add(getOccupancy(oldStack).multiplyBy(Fraction.getFraction(oldStack.getCount(), 1)));
                }
            }
            return Math.max(Fraction.ONE.subtract(occupancy).intValue(), 0);
        }

        @Override
        public int getSpace(ItemStack stack) {
            Fraction space = Fraction.ONE.subtract(getOccupancy());
            return Math.max(space.divideBy(getOccupancy(stack)).intValue(), 0);
        }

        @Override
        public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
            ItemStack result = ItemStack.EMPTY;
            List<ItemStack> stacks = Lists.newArrayList(getStacksCopy());
            for (ItemStack stack : stacks) {
                if (filter.matches(stack)) {
                    result = stack.split(maxAmount);
                }
            }
            stacks.removeIf(ItemStack::isEmpty);
            if (simulation.isAction()) {
                ItemStack stack = ref.get();
                stack.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(stacks));
                ref.set(stack);
            }
            return result;
        }

        @Override
        public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
            BundleContentsComponent component = ref.get().getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
            BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(component);
            stack = stack.copy();
            builder.add(stack);
            if (simulation.isAction()) {
                ItemStack ourStack = ref.get();
                ourStack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                ref.set(ourStack);
            }
            return stack;
        }

        private Iterable<ItemStack> getStacks() {
            ItemStack stack = ref.get();
            BundleContentsComponent component = stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
            return component.iterate();
        }
        
        private Iterable<ItemStack> getStacksCopy() {
            ItemStack stack = ref.get();
            BundleContentsComponent component = stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
            return component.iterateCopy();
        }

        private Fraction getOccupancy() {
            ItemStack stack = ref.get();
            BundleContentsComponent component = stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
            return component.getOccupancy();
        }

        private static Fraction getOccupancy(ItemStack stack) {
            BundleContentsComponent component = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (component != null) {
                return NESTED_BUNDLE_OCCUPANCY.add(component.getOccupancy());
            } else {
                List<BeehiveBlockEntity.BeeData> list = stack.getOrDefault(DataComponentTypes.BEES, List.of());
                return !list.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, stack.getMaxCount());
            }
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
        ModContainer itemsModule = LbaModule.ITEMS.getModContainer();

        if (itemsModule == null || coreModule == null) {
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

        Class<?> itemsClass = FluidAttributes.class;
        Class<?> coreClass = Attribute.class;
        URL itemsLoc = itemsClass.getProtectionDomain().getCodeSource().getLocation();
        URL coreLoc = coreClass.getProtectionDomain().getCodeSource().getLocation();

        if (itemsLoc.equals(coreLoc)) {
            // 9
            throw new Error("(core and items have the same path " + itemsLoc + ")" + FatJarChecker.FATJAR_ERROR);
        }

        // 7
        return;
    }
}
