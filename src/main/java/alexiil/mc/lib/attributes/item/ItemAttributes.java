/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.net.URL;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.AttributeCombiner;
import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.CustomAttributeAdder;
import alexiil.mc.lib.attributes.DefaultedAttribute;
import alexiil.mc.lib.attributes.ItemAttributeAdder;
import alexiil.mc.lib.attributes.ItemAttributeList;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fatjar.FatJarChecker;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.item.FixedItemInv.CopyingFixedItemInv;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.compat.FixedSidedInventoryVanillaWrapper;
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
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;

public final class ItemAttributes {
    private ItemAttributes() {}

    public static final CombinableAttribute<FixedItemInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<FixedItemInv> FIXED_INV;
    public static final CombinableAttribute<GroupedItemInvView> GROUPED_INV_VIEW;
    public static final CombinableAttribute<GroupedItemInv> GROUPED_INV;
    public static final CombinableAttribute<ItemInsertable> INSERTABLE;
    public static final CombinableAttribute<ItemExtractable> EXTRACTABLE;

    /** Mostly intended to be used for {@link ItemStack}'s, not {@link Block}'s. (As this interface doesn't really make
     * much sense when applied to block's alone, however it makes much more sense in pipe input or extraction
     * filters). */
    public static final CombinableAttribute<ItemFilter> FILTER;

    static {
        FIXED_INV_VIEW = create(
            FixedItemInvView.class, //
            EmptyFixedItemInv.INSTANCE, //
            list -> new CombinedFixedItemInvView<>(list), //
            inv -> inv//
        );
        FIXED_INV = create(
            FixedItemInv.class, //
            EmptyFixedItemInv.INSTANCE, //
            list -> new CombinedFixedItemInv<>(list), //
            Function.identity()//
        );
        GROUPED_INV_VIEW = create(
            GroupedItemInvView.class, //
            EmptyGroupedItemInv.INSTANCE, //
            list -> new CombinedGroupedItemInvView(list), //
            FixedItemInv::getGroupedInv//
        );
        GROUPED_INV = create(
            GroupedItemInv.class, //
            EmptyGroupedItemInv.INSTANCE, //
            list -> new CombinedGroupedItemInv(list), //
            FixedItemInv::getGroupedInv//
        );
        INSERTABLE = create(
            ItemInsertable.class, //
            RejectingItemInsertable.NULL, //
            list -> new CombinedItemInsertable(list), //
            FixedItemInv::getInsertable//
        );
        EXTRACTABLE = create(
            ItemExtractable.class, //
            EmptyItemExtractable.NULL, //
            list -> new CombinedItemExtractable(list), //
            FixedItemInv::getExtractable//
        );
        FILTER = Attributes.createCombinable(
            ItemFilter.class, //
            ConstantItemFilter.NOTHING, //
            list -> AggregateItemFilter.allOf(list)//
        );
    }

    private static <T> CombinableAttribute<T> create(
        Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner, Function<FixedItemInv, T> convertor
    ) {

        return Attributes.createCombinable(clazz, defaultValue, combiner)//
            .appendBlockAdder(createBlockAdder(convertor))//
            .appendItemAdder(createItemAdder(convertor))//
        ;
    }

    private static <T> CustomAttributeAdder<T> createBlockAdder(Function<FixedItemInv, T> convertor) {
        return (world, pos, state, list) -> {
            Block block = state.getBlock();
            Direction direction = list.getSearchDirection();
            Direction blockSide = direction == null ? null : direction.getOpposite();

            if (block instanceof InventoryProvider) {
                InventoryProvider provider = (InventoryProvider) block;
                SidedInventory inventory = provider.getInventory(state, world, pos);
                if (inventory != null) {
                    if (inventory.getInvSize() > 0) {
                        final FixedItemInv wrapper;
                        if (direction != null) {
                            wrapper = FixedSidedInventoryVanillaWrapper.create(inventory, blockSide);
                        } else {
                            wrapper = new FixedInventoryVanillaWrapper(inventory);
                        }
                        list.add(convertor.apply(wrapper));
                    } else {
                        list.add(((DefaultedAttribute<T>) list.attribute).defaultValue);
                    }
                }
            } else if (block.hasBlockEntity()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof ChestBlockEntity && state.getBlock() instanceof ChestBlock) {
                    // Special case chests here, rather than through a mixin because it just simplifies
                    // everything

                    boolean checkForBlockingCats = false;
                    Inventory chestInv = ChestBlock
                        .getInventory((ChestBlock) state.getBlock(), state, world, pos, checkForBlockingCats);
                    if (chestInv != null) {
                        list.add(convertor.apply(new FixedInventoryVanillaWrapper(chestInv)));
                    }
                } else if (be instanceof SidedInventory) {
                    SidedInventory sidedInv = (SidedInventory) be;
                    final FixedItemInv wrapper;
                    if (direction != null) {
                        wrapper = FixedSidedInventoryVanillaWrapper.create(sidedInv, blockSide);
                    } else {
                        wrapper = new FixedInventoryVanillaWrapper(sidedInv);
                    }
                    list.add(convertor.apply(wrapper));
                } else if (be instanceof Inventory) {
                    list.add(convertor.apply(new FixedInventoryVanillaWrapper((Inventory) be)));
                }
            }
        };
    }

    private static <T> ItemAttributeAdder<T> createItemAdder(Function<FixedItemInv, T> convertor) {
        return (ref, excess, list) -> appendItemAttributes(ref, excess, list, convertor);
    }

    private static <T> void appendItemAttributes(
        Reference<ItemStack> ref, LimitedConsumer<ItemStack> excess, ItemAttributeList<T> list,
        Function<FixedItemInv, T> convertor
    ) {

        ItemStack stack = ref.get();

        if (isShulkerBox(stack)) {
            list.add(convertor.apply(new ShulkerBoxItemInv(ref)));
        }
    }

    static boolean isShulkerBox(ItemStack stack) {
        return Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }

    static final class ShulkerBoxItemInv implements CopyingFixedItemInv {
        private final Reference<ItemStack> ref;
        private int changes = 0;

        private ShulkerBoxItemInv(Reference<ItemStack> ref) {
            this.ref = ref;
        }

        @Override
        public int getSlotCount() {
            return 27;
        }

        @Override
        public ItemStack getInvStack(int slot) {
            assert 0 <= slot && slot < 27;

            ItemStack stack = ref.get();
            CompoundTag tag = stack.getSubTag("BlockEntityTag");
            if (tag == null || stack.isEmpty() || stack.getCount() != 1 || !isShulkerBox(stack)) {
                return ItemStack.EMPTY;
            }

            DefaultedList<ItemStack> list = DefaultedList.of();
            Inventories.fromTag(tag, list);
            if (slot >= list.size()) {
                return ItemStack.EMPTY;
            }
            return list.get(slot);
        }

        @Override
        public ItemStack getUnmodifiableInvStack(int slot) {
            // Because we deserialise every time it's safe to just return it
            return getInvStack(slot);
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            // Check for grouped item inv because everything else boils down to this
            // (Plus we don't care about insertable or extractable's, only inventories)
            return stack.isEmpty() || ItemAttributes.GROUPED_INV_VIEW.getFirstOrNull(stack) == null;
        }

        @Override
        public int getChangeValue() {
            // We don't have access to whatever the reference is, so we can't be certain about when it's changed
            return changes++;
        }

        @Override
        public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
            // Listeners don't make any sense.
            return null;
        }

        @Override
        public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
            if (slot <= 0 || slot > 27) {
                return false;
            }

            if (!isItemValidForSlot(slot, to)) {
                return false;
            }

            ItemStack stack = ref.get();
            if (!stack.isEmpty() || stack.getCount() != 1 || !isShulkerBox(stack)) {
                return false;
            }

            if (simulation == Simulation.ACTION) {
                stack = stack.copy();
            }

            CompoundTag tag = stack.getSubTag("BlockEntityTag");
            if (tag == null) {
                if (simulation == Simulation.ACTION) {
                    tag = stack.getOrCreateSubTag("BlockEntityTag");
                } else {
                    tag = new CompoundTag();
                }
            } else if (simulation == Simulation.SIMULATE) {
                tag = new CompoundTag().copyFrom(tag);
            }

            DefaultedList<ItemStack> list = DefaultedList.of();
            Inventories.fromTag(tag, list);

            while (slot >= list.size()) {
                list.add(ItemStack.EMPTY);
            }

            list.set(slot, to);
            Inventories.toTag(tag, list);
            return ref.set(stack, simulation);
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
