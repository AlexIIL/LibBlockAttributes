/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.CustomAttributeAdder;
import alexiil.mc.lib.attributes.DefaultedAttribute;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.compat.FixedSidedInventoryVanillaWrapper;
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

public final class ItemAttributes {
    private ItemAttributes() {}

    public static final CombinableAttribute<FixedItemInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<FixedItemInv> FIXED_INV;
    public static final CombinableAttribute<GroupedItemInvView> GROUPED_INV_VIEW;
    public static final CombinableAttribute<GroupedItemInv> GROUPED_INV;
    public static final CombinableAttribute<ItemInsertable> INSERTABLE;
    public static final CombinableAttribute<ItemExtractable> EXTRACTABLE;

    static {
        FIXED_INV_VIEW = Attributes.createCombinable(
            //
            FixedItemInvView.class, //
            EmptyFixedItemInv.INSTANCE, //
            list -> new CombinedFixedItemInvView<>(list), //
            createFixedInvAdder(inv -> inv)//
        );
        FIXED_INV = Attributes.createCombinable(
            //
            FixedItemInv.class, //
            EmptyFixedItemInv.INSTANCE, //
            list -> new CombinedFixedItemInv<>(list), //
            createFixedInvAdder(Function.identity())//
        );
        GROUPED_INV_VIEW = Attributes.createCombinable(
            //
            GroupedItemInvView.class, //
            EmptyGroupedItemInv.INSTANCE, //
            list -> new CombinedGroupedItemInvView(list), //
            createFixedInvAdder(FixedItemInv::getGroupedInv)//
        );
        GROUPED_INV = Attributes.createCombinable(
            //
            GroupedItemInv.class, //
            EmptyGroupedItemInv.INSTANCE, //
            list -> new CombinedGroupedItemInv(list), //
            createFixedInvAdder(FixedItemInv::getGroupedInv)//
        );
        INSERTABLE = Attributes.createCombinable(
            //
            ItemInsertable.class, //
            RejectingItemInsertable.NULL, //
            list -> new CombinedItemInsertable(list), //
            createFixedInvAdder(FixedItemInv::getInsertable)//
        );
        EXTRACTABLE = Attributes.createCombinable(
            //
            ItemExtractable.class, //
            EmptyItemExtractable.NULL, //
            list -> new CombinedItemExtractable(list), //
            createFixedInvAdder(FixedItemInv::getExtractable)//
        );
    }

    private static <T> CustomAttributeAdder<T> createFixedInvAdder(Function<FixedItemInv, T> getter) {
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
                        list.add(getter.apply(wrapper));
                    } else {
                        list.add(((DefaultedAttribute<T>) list.attribute).defaultValue);
                    }
                }
            } else if (block.hasBlockEntity()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof ChestBlockEntity) {
                    // Special case chests here, rather than through a mixin because it just simplifies
                    // everything

                    boolean checkForBlockingCats = false;
                    Inventory chestInv = ChestBlock.getInventory(state, world, pos, checkForBlockingCats);
                    if (chestInv != null) {
                        list.add(getter.apply(new FixedInventoryVanillaWrapper(chestInv)));
                    }
                } else if (be instanceof SidedInventory) {
                    SidedInventory sidedInv = (SidedInventory) be;
                    final FixedItemInv wrapper;
                    if (direction != null) {
                        wrapper = FixedSidedInventoryVanillaWrapper.create(sidedInv, blockSide);
                    } else {
                        wrapper = new FixedInventoryVanillaWrapper(sidedInv);
                    }
                    list.add(getter.apply(wrapper));
                } else if (be instanceof Inventory) {
                    list.add(getter.apply(new FixedInventoryVanillaWrapper((Inventory) be)));
                }
            }
        };
    }
}
