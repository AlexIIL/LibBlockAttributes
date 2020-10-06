/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv.CopyingFixedItemInv;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.PlayerInvUtil;
import alexiil.mc.lib.attributes.misc.Reference;

/** Various hooks and methods for dealing with pairs of {@link FixedItemInv}, {@link FixedItemInvView},
 * {@link ItemInsertable}, {@link ItemExtractable}, and {@link GroupedItemInvView} instances. */
public final class ItemInvUtil {
    private ItemInvUtil() {}

    // #######################
    // Direct utility methods
    // #######################

    /** Returns a {@link Consumer} that will call
     * {@link PlayerInvUtil#insertItemIntoPlayerInventory(PlayerEntity, ItemStack)} for every {@link ItemStack} passed
     * to it.
     * 
     * @deprecated Use {@link PlayerInvUtil#createPlayerInsertable(PlayerEntity)} instead */
    @Deprecated // (since = "0.6.4", forRemoval = true)
    public static Consumer<ItemStack> createPlayerInsertable(PlayerEntity player) {
        return PlayerInvUtil.createPlayerInsertable(player);
    }

    /** Creates a {@link Reference} to the what the player is currently holding in the given {@link Hand}.
     * 
     * @deprecated Use {@link PlayerInvUtil#referenceHand(PlayerEntity,Hand)} instead */
    @Deprecated // (since = "0.6.4", forRemoval = true)
    public static Reference<ItemStack> referenceHand(PlayerEntity player, Hand hand) {
        return PlayerInvUtil.referenceHand(player, hand);
    }

    /** Creates a {@link Reference} to the given player's {@link PlayerInventory#getCursorStack() cursor stack}, that
     * updates the client whenever it is changed.
     * 
     * @deprecated Use {@link PlayerInvUtil#referenceGuiCursor(ServerPlayerEntity)} instead */
    @Deprecated // (since = "0.6.4", forRemoval = true)
    public static Reference<ItemStack> referenceGuiCursor(ServerPlayerEntity player) {
        return PlayerInvUtil.referenceGuiCursor(player);
    }

    /** Either inserts the given item into the player's inventory or drops it in front of them. Note that this will
     * always keep a reference to the passed stack (and might modify it!)
     * 
     * @deprecated Use {@link PlayerInvUtil#insertItemIntoPlayerInventory(PlayerEntity,ItemStack)} instead */
    @Deprecated // (since = "0.6.4", forRemoval = true)
    public static void insertItemIntoPlayerInventory(PlayerEntity player, ItemStack stack) {
        PlayerInvUtil.insertItemIntoPlayerInventory(player, stack);
    }

    /** Attempts to move up to the given maximum number of items from the {@link ItemExtractable} to the
     * {@link ItemInsertable}. Note that this only ever moves a single stack, unlike
     * {@link #moveMultiple(ItemExtractable, ItemInsertable, ItemFilter, int, int)}.
     * 
     * @return The number of items moved.
     * @see #move(ItemExtractable, ItemInsertable, ItemFilter, int) */
    public static int move(ItemExtractable from, ItemInsertable to, int maximum) {
        return move(from, to, null, maximum);
    }

    /** Attempts to move up to the given maximum number of items from the {@link ItemExtractable} to the
     * {@link ItemInsertable}, provided they match the given {@link ItemFilter}. Note that this only ever moves a single
     * stack, unlike {@link #moveMultiple(ItemExtractable, ItemInsertable, ItemFilter, int, int)}.
     * 
     * @return The number of items moved. */
    public static int move(ItemExtractable from, ItemInsertable to, ItemFilter filter, int maximum) {
        return move(from, to, filter, maximum, Simulation.ACTION);
    }

    /** Attempts to move up to the given maximum number of items from the {@link ItemExtractable} to the
     * {@link ItemInsertable}, provided they match the given {@link ItemFilter}. Note that this only ever moves a single
     * stack, unlike {@link #moveMultiple(ItemExtractable, ItemInsertable, ItemFilter, int, int)}.
     * 
     * @return The number of items moved. */
    public static int move(
        ItemExtractable from, ItemInsertable to, ItemFilter filter, int maximum, Simulation simulation
    ) {
        if (maximum <= 0) {
            return 0;
        }
        ItemFilter insertionFilter = to.getInsertionFilter();
        if (filter != null && filter != ConstantItemFilter.ANYTHING) {
            insertionFilter = AggregateItemFilter.and(insertionFilter, filter);
        }

        ItemStack extracted = from.attemptExtraction(insertionFilter, maximum, Simulation.SIMULATE);
        if (extracted.isEmpty()) {
            return 0;
        }
        ItemStack leftover = to.attemptInsertion(extracted, simulation);
        int insertedAmount = extracted.getCount() - (leftover.isEmpty() ? 0 : leftover.getCount());
        if (insertedAmount == 0) {
            return 0; // Nothing was accepted by the target
        }
        ItemStack reallyExtracted
            = from.attemptExtraction(new ExactItemStackFilter(extracted), insertedAmount, simulation);

        if (reallyExtracted.isEmpty()) {
            throw throwBadImplException(
                "Tried to extract the filter (C) from A but it returned an empty item stack "
                    + "after we have already inserted the expected stack into B!\nThe inventory is now in an invalid (duped) state!",
                new String[] { "from A", "to B", "filter C" }, new Object[] { from, to, filter }
            );
        }
        if (reallyExtracted.getCount() != insertedAmount) {
            throw throwBadImplException(
                "Tried to extract " + insertedAmount + " but we actually extracted " + reallyExtracted.getCount()
                    + "!\nThe inventory is now in an invalid (duped) state!",
                new String[] { "from A", "to B", "filter C", "originally extracted", "really extracted" },
                new Object[] { from, to, insertionFilter, extracted, reallyExtracted }
            );
        }
        return insertedAmount;
    }

    /** Attempts to move as much as possible from the {@link ItemExtractable} to the {@link ItemInsertable}. Internally
     * this calls {@link #moveMultiple(ItemExtractable, ItemInsertable, int, int)} with {@link Integer#MAX_VALUE} as the
     * maximum value for both arguments.
     * 
     * @return The {@link MultiMoveResult} */
    public static MultiMoveResult moveMultiple(ItemExtractable from, ItemInsertable to) {
        return moveMultiple(from, to, ConstantItemFilter.ANYTHING, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** Attempts to move a given number of stacks from the {@link ItemExtractable} to the {@link ItemInsertable}.
     * Internally this calls {@link #move(ItemExtractable, ItemInsertable, int)} in a loop from 1 to maxStacks.
     * 
     * @return The {@link MultiMoveResult} */
    public static MultiMoveResult moveMultiple(ItemExtractable from, ItemInsertable to, int maxStacks, int maxTotal) {
        return moveMultiple(from, to, ConstantItemFilter.ANYTHING, maxStacks, maxTotal);
    }

    /** Attempts to move a given number of stacks from the {@link ItemExtractable} to the {@link ItemInsertable}.
     * Internally this calls {@link #move(ItemExtractable, ItemInsertable, int)} in a loop from 1 to maxStacks.
     * 
     * @return The {@link MultiMoveResult} */
    public static MultiMoveResult moveMultiple(
        ItemExtractable from, ItemInsertable to, ItemFilter filter, int maxStacks, int maxTotal
    ) {
        int itemsMoved = 0;
        int stacks;
        for (stacks = 0; stacks < maxStacks; stacks++) {
            int moved = move(from, to, filter, maxTotal - itemsMoved);
            if (moved <= 0) {
                break;
            }
            itemsMoved += moved;
        }
        return new MultiMoveResult(stacks, itemsMoved);
    }

    /** A pair of ints, representing both the total number of stacks and the total number of items moved by
     * {@link ItemInvUtil#moveMultiple(ItemExtractable, ItemInsertable, ItemFilter, int, int)}. */
    public static final class MultiMoveResult {
        public final int stacksMoved;
        public final int itemsMoved;

        public MultiMoveResult(int stacksMoved, int itemsMoved) {
            this.stacksMoved = stacksMoved;
            this.itemsMoved = itemsMoved;
        }

        /** @return True if {@link #itemsMoved} is greater than 0. */
        public boolean didMoveAny() {
            return itemsMoved > 0;
        }
    }

    /** Copies every {@link ItemStack} held in the given inventory to the given {@link List}. */
    public static void copyAll(FixedItemInvView inv, List<ItemStack> dest) {
        for (int slot = 0; slot < inv.getSlotCount(); slot++) {
            ItemStack stack = inv.getInvStack(slot);
            if (!(inv instanceof CopyingFixedItemInv)) {
                stack = stack.copy();
            }
            if (!stack.isEmpty()) {
                dest.add(stack);
            }
        }
    }

    // #######################
    // Implementation helpers
    // #######################

    /** Inserts a single ItemStack into a {@link FixedItemInv}, using only
     * {@link FixedItemInv#setInvStack(int, ItemStack, Simulation)}. As such this is useful for implementations of
     * {@link ItemInsertable} (or others) for the base implementation.
     * 
     * @param toInsert The stack to insert. This won't be modified.
     * @return The excess {@link ItemStack} that wasn't inserted.
     * @deprecated Because this has been moved to {@link FixedItemInv#insertStack(int, ItemStack, Simulation)}. */
    @Deprecated // (since = "0.8.0", forRemoval = true)
    public static ItemStack insertSingle(FixedItemInv inv, int slot, ItemStack toInsert, Simulation simulation) {
        return inv.insertStack(slot, toInsert, simulation);
    }

    /** Extracts a single ItemStack from a {@link FixedItemInv}, using only
     * {@link FixedItemInv#setInvStack(int, ItemStack, Simulation)}. As such this is useful for implementations of
     * {@link ItemInsertable} (or others) for the base implementation.
     * 
     * @param filter The filter to match on. If this is null then it will match any {@link ItemStack}.
     * @param toAddWith An optional {@link ItemStack} that the extracted item will be added to.
     * @param maxAmount The maximum number of items to extract. Note that the returned {@link ItemStack} may have a
     *            higher amount than this if the given {@link ItemStack} isn't empty.
     * @return The extracted ItemStack, plus the parameter "toAddWith".
     * @deprecated Because this has been moved to
     *             {@link FixedItemInv#extractStack(int, ItemFilter, ItemStack, int, Simulation)}. */
    @Deprecated // (since = "0.8.0", forRemoval = true)
    public static ItemStack extractSingle(
        FixedItemInv inv, int slot, @Nullable ItemFilter filter, ItemStack toAddWith, int maxAmount,
        Simulation simulation
    ) {
        return inv.extractStack(slot, filter, toAddWith, maxAmount, simulation);
    }

    // #######################
    // Private Util
    // #######################

    private static IllegalStateException throwBadImplException(String reason, String[] names, Object[] objs) {
        String detail = "\n";
        int max = Math.max(names.length, objs.length);
        for (int i = 0; i < max; i++) {
            String name = names.length <= i ? "?" : names[i];
            Object obj = objs.length <= i ? "" : objs[i];
            // TODO: Full object detail!
            detail += "\n" + name + " = " + obj;
        }
        throw new IllegalStateException(reason + detail);
    }
}
