/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.SearchOption;
import alexiil.mc.lib.attributes.SearchOptions;
import alexiil.mc.lib.attributes.item.ItemAttributes;
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;

/** Helper class for the {@link HopperBlockEntity} to implement the actual transfer logic. */
public final class HopperHooks {

    /** Hoppers always search UP for adjacent inventories to extract from. */
    private static final SearchOption<? super ItemExtractable> EXTRACT_SEARCH = SearchOptions.inDirection(Direction.UP);

    private HopperHooks() {}

    /** Attempts to insert items from a Hopper into an adjacent LBA insertable. If the block entity to be inserted into
     * directly implements {@link Inventory}, we delegate back to Vanilla.
     *
     * @return {@link ActionResult#PASS} to continue with the original Vanilla logic, otherwise indicates whether
     *         insertion failed or succeeded. */
    public static ActionResult tryInsert(HopperBlockEntity hopper) {
        Direction towards = hopper.getCachedState().get(HopperBlock.FACING);
        BlockPos targetPos = hopper.getPos().offset(towards);

        World world = hopper.getWorld();
        if (isVanillaInventoryAt(world, targetPos)) {
            return ActionResult.PASS;
        }

        ItemInsertable insertable = ItemAttributes.INSERTABLE.get(world, targetPos, SearchOptions.inDirection(towards));
        if (insertable == RejectingItemInsertable.NULL) {
            return ActionResult.PASS; // Let Vanilla handle non-LBA enabled inventories and Entities
        }

        // Get an Extractable for the Hopper's internal inventory
        ItemExtractable extractable = new FixedInventoryVanillaWrapper(hopper).getExtractable();

        // Try to move any one item from hopper->inventory
        if (ItemInvUtil.move(extractable, insertable, 1) > 0) {
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.FAIL;
        }
    }

    /** Tries to extract items from a LBA extractable above the given hopper. Note that the given hopper can also be a
     * hopper minecart.
     * <p>
     * If the block entity to be extracted from directly implements {@link Inventory}, we delegate back to Vanilla.
     *
     * @return {@link ActionResult#PASS} to continue with the original Vanilla logic, otherwise indicates whether
     *         extraction failed or succeeded. */
    public static ActionResult tryExtract(World world, Hopper hopper) {
        BlockPos blockAbove = new BlockPos(hopper.getHopperX(), hopper.getHopperY() + 1, hopper.getHopperZ());

        if (isVanillaInventoryAt(world, blockAbove)) {
            return ActionResult.PASS;
        }

        // Get an Extractable for the inventory above the hopper
        ItemExtractable extractable = ItemAttributes.EXTRACTABLE.get(world, blockAbove, EXTRACT_SEARCH);
        if (extractable == EmptyItemExtractable.NULL) {
            return ActionResult.PASS; // Let Vanilla handle non-LBA enabled inventories and Entities
        }

        // Get an Insertable for the Hopper's internal inventory
        ItemInsertable insertable = new FixedInventoryVanillaWrapper(hopper).getInsertable();

        // Try to move any one item from inventory->hopper
        if (ItemInvUtil.move(extractable, insertable, 1) > 0) {
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.FAIL;
        }
    }

    public static ActionResult tryDispense(DispenserBlockEntity dropper, int invIndex) {
        if (invIndex < 0) {
            return ActionResult.PASS;
        }
        Direction towards = dropper.getCachedState().get(DispenserBlock.FACING);
        BlockPos targetPos = dropper.getPos().offset(towards);

        World world = dropper.getWorld();
        if (isVanillaInventoryAt(world, targetPos)) {
            return ActionResult.PASS;
        }

        ItemInsertable insertable = ItemAttributes.INSERTABLE.get(world, targetPos, SearchOptions.inDirection(towards));
        if (insertable == RejectingItemInsertable.NULL) {
            return ActionResult.PASS; // Let Vanilla handle non-LBA enabled inventories and Entities
        }

        // Get an Extractable for the Hopper's internal inventory
        ItemExtractable extractable = new FixedInventoryVanillaWrapper(dropper).getSlot(invIndex);

        // Try to move any one item from hopper->inventory
        if (ItemInvUtil.move(extractable, insertable, 1) > 0) {
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.FAIL;
        }
    }

    private static boolean isVanillaInventoryAt(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        if ("minecraft".equals(Registries.BLOCK.getId(block).getNamespace())) {
            return true;
        }
        return false;
        // If there's a TE at the target position that implements Inventory (such that Hopper would handle it itself)
        // defer to Vanilla to avoid injecting ourselves inbetween Vanilla blocks needlessly.
        // ItemAttributes.INSERTABLE would return an auto-converted Inventory in such cases.
        // return world.getBlockEntity(pos) instanceof Inventory;
    }
}
