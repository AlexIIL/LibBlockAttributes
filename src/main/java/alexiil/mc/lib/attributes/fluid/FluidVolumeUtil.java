/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.math.RoundingMode;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Ref;
import alexiil.mc.lib.attributes.misc.Reference;

public final class FluidVolumeUtil {
    private FluidVolumeUtil() {}

    public static final FluidVolume EMPTY = FluidKeys.EMPTY.withAmount(FluidAmount.ZERO);

    /** Attempts to move as much fluid as possible from the {@link FluidExtractable} to the {@link FluidInsertable}.
     * 
     * @return A copy of the fluid moved.
     * @see #move(FluidExtractable, FluidInsertable, FluidFilter, int) */
    public static FluidVolume move(FluidExtractable from, FluidInsertable to) {
        return move(from, to, null, FluidAmount.MAX_VALUE);
    }

    /** Attempts to move up to the given amount of fluid from the {@link FluidExtractable} to the
     * {@link FluidInsertable}.
     * 
     * @return A copy of the fluid moved.
     * @see #move(FluidExtractable, FluidInsertable, FluidFilter, int)
     * @deprecated Replaced by {@link #move(FluidExtractable, FluidInsertable, FluidAmount)} */
    @Deprecated
    public static FluidVolume move(FluidExtractable from, FluidInsertable to, int maximum) {
        return move(from, to, null, maximum);
    }

    /** Attempts to move up to the given amount of fluid from the {@link FluidExtractable} to the
     * {@link FluidInsertable}.
     * 
     * @return A copy of the fluid moved.
     * @see #move(FluidExtractable, FluidInsertable, FluidFilter, int) */
    public static FluidVolume move(FluidExtractable from, FluidInsertable to, FluidAmount maximum) {
        return move(from, to, null, maximum);
    }

    /** Attempts to move up to the given maximum amount of fluids from the {@link FluidExtractable} to the
     * {@link FluidInsertable}, provided they match the given {@link FluidFilter}.
     * 
     * @return A copy of the fluid moved.
     * @deprecated Replaced by {@link #move(FluidExtractable, FluidInsertable, FluidFilter, FluidAmount)} */
    @Deprecated
    public static FluidVolume move(FluidExtractable from, FluidInsertable to, FluidFilter filter, int maximum) {
        return move(from, to, filter, FluidAmount.of1620(maximum));
    }

    /** Attempts to move up to the given maximum amount of fluids from the {@link FluidExtractable} to the
     * {@link FluidInsertable}, provided they match the given {@link FluidFilter}.
     * 
     * @return A copy of the fluid moved. */
    public static FluidVolume move(FluidExtractable from, FluidInsertable to, FluidFilter filter, FluidAmount maximum) {
        FluidFilter insertionFilter = to.getInsertionFilter();
        if (filter != null && filter != ConstantFluidFilter.ANYTHING) {
            insertionFilter = AggregateFluidFilter.and(insertionFilter, filter);
        }

        // 5 steps:
        // 1: (Simulate) Try to extract as much as possible, to find out the maximum amount of fluid available
        // 2: (Simulate) Try to insert as much of the extracted as possible
        // 3: (Simulate) Try to extract the exact amount that was actually inserted
        /* We don't need to simulate inserting the exact amount because it should always be safe to insert the amount
         * minus the leftover. */
        // If all of the above steps provide an exact amount > 0:
        // 4: Extract the exact amount
        // 5: Insert the exact fluid.
        // and assert that there is no leftover.

        // Step 1:
        FluidVolume extracted = from.attemptExtraction(insertionFilter, maximum, Simulation.SIMULATE);
        if (extracted.isEmpty()) {
            return EMPTY;
        }

        // Step 2:
        FluidVolume firstLeftover = to.attemptInsertion(extracted, Simulation.SIMULATE);
        FluidAmount firstInserted = extracted.getAmount_F().roundedSub(firstLeftover.getAmount_F());
        if (!firstInserted.isPositive()) {
            return EMPTY;
        }

        // Step 3:
        FluidVolume exactExtracted
            = from.attemptExtraction(new ExactFluidFilter(extracted.fluidKey), firstInserted, Simulation.SIMULATE);
        if (!exactExtracted.getAmount_F().equals(firstInserted)) {
            return EMPTY;
        }

        // Step 4:
        FluidVolume reallyExtracted = from.extract(exactExtracted.fluidKey, firstInserted);
        if (!reallyExtracted.equals(exactExtracted)) {
            throw throwBadImplException(
                "A simulated extraction (returning A) didn't match the real extraction (returning B) from the fluid extractable C!",
                new String[] { "fluid A", "fluid B", "from C", "filter D" },
                new Object[] { exactExtracted, reallyExtracted, from, insertionFilter }
            );
        }

        // Step 5:
        FluidVolume leftover = to.insert(reallyExtracted);
        if (leftover.isEmpty()) {
            return reallyExtracted;
        }

        throw throwBadImplException(
            "A simulated insertion (of A returning B) didn't match the real insertion (of C returning D) into the fluid insertable E!",
            new String[] { "inserted A", "leftover B", "inserted C", "leftover D", "insertable E" },
            new Object[] { extracted, firstLeftover, reallyExtracted, leftover, to }
        );
    }

    /** @return An {@link FluidInsertable} that will insert fluids into the given stack (overflowing into the given
     *         {@link Consumer})
     * @deprecated This has been replaced by the item-based attributes system. */
    @Deprecated
    public static FluidInsertable createItemInventoryInsertable(
        Ref<ItemStack> stackRef, Consumer<ItemStack> excessStacks
    ) {

        return FluidAttributes.INSERTABLE.get(stackRef, LimitedConsumer.fromConsumer(excessStacks));
    }

    public static FluidExtractable createItemInventoryExtractable(
        Ref<ItemStack> stackRef, Consumer<ItemStack> excessStacks
    ) {

        return FluidAttributes.EXTRACTABLE.get(stackRef, LimitedConsumer.fromConsumer(excessStacks));
    }

    public static boolean interactWithTank(FixedFluidInv inv, PlayerEntity player, Hand hand) {
        return interactWithTank(inv.getInsertable(), inv.getExtractable(), player, hand);
    }

    public static boolean interactWithTank(FluidTransferable inv, PlayerEntity player, Hand hand) {
        return interactWithTank(inv, inv, player, hand);
    }

    public static boolean interactWithTank(
        FluidInsertable invInsert, FluidExtractable invExtract, PlayerEntity player, Hand hand
    ) {

        return interactWithTank(invInsert, invExtract, player, ItemInvUtil.referenceHand(player, hand));
    }

    public static boolean interactCursorWithTank(FixedFluidInv inv, ServerPlayerEntity player) {
        return interactCursorWithTank(inv.getInsertable(), inv.getExtractable(), player);
    }

    public static boolean interactCursorWithTank(FluidTransferable inv, ServerPlayerEntity player) {
        return interactCursorWithTank(inv, inv, player);
    }

    /** Interacts with a tank from the player's cursor stack when there is a gui open. */
    public static boolean interactCursorWithTank(
        FluidInsertable invInsert, FluidExtractable invExtract, ServerPlayerEntity player
    ) {

        return interactWithTank(invInsert, invExtract, player, ItemInvUtil.referenceGuiCursor(player));
    }

    public static boolean interactWithTank(
        FluidInsertable invInsert, FluidExtractable invExtract, PlayerEntity player, Reference<ItemStack> mainStackRef
    ) {

        ItemStack mainStack = mainStackRef.get();
        if (mainStack.isEmpty()) {
            return false;
        }
        boolean isSurvival = !player.abilities.creativeMode;
        Reference<ItemStack> realRef
            = isSurvival ? mainStackRef : Reference.callable(mainStackRef::get, s -> {}, s -> true);
        Consumer<ItemStack> stackConsumer = isSurvival ? ItemInvUtil.createPlayerInsertable(player) : s -> {};
        FluidTankInteraction result
            = interactWithTank(invInsert, invExtract, realRef, LimitedConsumer.fromConsumer(stackConsumer));
        if (!result.didMoveAny()) {
            return false;
        }
        final SoundEvent soundEvent;
        if (result.fluidMoved.fluidKey == FluidKeys.LAVA) {
            soundEvent = result.intoTank ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_FILL_LAVA;
        } else {
            boolean isBottle
                = mainStack.getItem() instanceof GlassBottleItem || mainStack.getItem() instanceof PotionItem;
            if (isBottle) {
                soundEvent = result.intoTank ? SoundEvents.ITEM_BOTTLE_EMPTY : SoundEvents.ITEM_BOTTLE_FILL;
            } else {
                soundEvent = result.intoTank ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_FILL;
            }
        }
        player.playSound(soundEvent, SoundCategory.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    /** @deprecated This has been replaced by {@link #interactWithTank(FixedFluidInv, Reference, LimitedConsumer)}. */
    @Deprecated
    public static FluidTankInteraction interactWithTank(
        FixedFluidInv inv, Ref<ItemStack> stack, Consumer<ItemStack> excessStacks
    ) {

        return interactWithTank(inv.getInsertable(), inv.getExtractable(), stack, excessStacks);
    }

    /** @deprecated This has been replaced by {@link #interactWithTank(FixedFluidInv, Reference, LimitedConsumer)}. */
    @Deprecated
    public static FluidTankInteraction interactWithTank(
        FluidTransferable inv, Ref<ItemStack> stack, Consumer<ItemStack> excessStacks
    ) {

        return interactWithTank(inv, inv, stack, excessStacks);
    }

    /** @param invInsert The fluid inventory to interact with
     * @param invExtract The fluid inventory to interact with
     * @param stack The held {@link ItemStack} to interact with.
     * @param excessStacks A {@link Consumer} to take the excess {@link ItemStack}'s.
     * @deprecated This has been replaced by {@link #interactWithTank(FixedFluidInv, Reference, LimitedConsumer)}. */
    public static FluidTankInteraction interactWithTank(
        FluidInsertable invInsert, FluidExtractable invExtract, Ref<ItemStack> stack, Consumer<ItemStack> excessStacks
    ) {

        return interactWithTank(invInsert, invExtract, stack, LimitedConsumer.fromConsumer(excessStacks));
    }

    /** @param inv The fluid inventory to interact with
     * @param stack The held {@link ItemStack} to interact with.
     * @param excessStacks A {@link Consumer} to take the excess {@link ItemStack}'s. */
    public static FluidTankInteraction interactWithTank(
        FixedFluidInv inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks
    ) {

        return interactWithTank(inv.getInsertable(), inv.getExtractable(), stack, excessStacks);
    }

    public static FluidTankInteraction interactWithTank(
        FluidTransferable inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks
    ) {

        return interactWithTank(inv, inv, stack, excessStacks);
    }

    public static FluidTankInteraction interactWithTank(
        FluidInsertable invInsert, FluidExtractable invExtract, Reference<ItemStack> stack,
        LimitedConsumer<ItemStack> excessStacks
    ) {

        FluidVolume fluidMoved = move(invExtract, FluidAttributes.INSERTABLE.get(stack, excessStacks));
        if (!fluidMoved.isEmpty()) {
            return FluidTankInteraction.fromTank(fluidMoved);
        }
        fluidMoved = move(FluidAttributes.EXTRACTABLE.get(stack, excessStacks), invInsert);
        return FluidTankInteraction.intoTank(fluidMoved);
    }

    public static final class FluidTankInteraction {
        public static final FluidTankInteraction NONE = new FluidTankInteraction(EMPTY, false);

        public final FluidVolume fluidMoved;
        public final boolean intoTank;

        public static FluidTankInteraction intoTank(FluidVolume fluid) {
            return new FluidTankInteraction(fluid, true);
        }

        public static FluidTankInteraction fromTank(FluidVolume fluid) {
            return new FluidTankInteraction(fluid, false);
        }

        public FluidTankInteraction(FluidVolume fluidMoved, boolean intoTank) {
            this.fluidMoved = fluidMoved;
            this.intoTank = intoTank;
        }

        public boolean didMoveAny() {
            return !fluidMoved.isEmpty();
        }

        @Deprecated
        public int amountMoved() {
            return fluidMoved.getAmount();
        }

        public FluidAmount amountMoved_F() {
            return fluidMoved.getAmount_F();
        }
    }

    // #######################
    // Implementation helpers
    // #######################

    /** Inserts a single {@link FluidVolume} into a {@link FixedFluidInv}, using only
     * {@link FixedFluidInv#setInvFluid(int, FluidVolume, Simulation)}. As such this is useful for implementations of
     * {@link FluidInsertable} (or others) for their base implementation.
     * 
     * @param toInsert The volume to insert. This will not be modified.
     * @return The excess {@link FluidVolume} that wasn't inserted. */
    public static FluidVolume insertSingle(FixedFluidInv inv, int tank, FluidVolume toInsert, Simulation simulation) {
        if (toInsert.isEmpty()) {
            return EMPTY;
        }
        FluidVolume inTank = inv.getInvFluid(tank);
        FluidAmount current = inTank.getAmount_F();
        FluidAmount max = current.roundedAdd(toInsert.getAmount_F(), RoundingMode.DOWN).min(inv.getMaxAmount_F(tank));
        FluidAmount addable = max.roundedSub(current, RoundingMode.UP);
        if (!addable.isPositive()) {
            return toInsert;
        }
        if (current.isPositive() && !inTank.canMerge(toInsert)) {
            return toInsert;
        }
        inTank = inTank.copy();
        FluidVolume insertCopy = toInsert.copy();
        FluidVolume merged = FluidVolume.merge(inTank, insertCopy.split(addable));
        if (merged == null) {
            return toInsert;
        }
        if (inv.setInvFluid(tank, merged, simulation)) {
            return insertCopy.isEmpty() ? EMPTY : insertCopy;
        }
        return toInsert;
    }

    /** Extracts a single {@link FluidVolume} from a {@link FixedFluidInv}, using only
     * {@link FixedFluidInv#setInvFluid(int, FluidVolume, Simulation)}. As such this is useful for implementations of
     * {@link FluidExtractable} (or others) for their base implementations.
     * 
     * @param filter The filter to match on. If this is null then it matches on anything.
     * @param toAddWith An optional {@link FluidVolume} that the extracted fluid will be added to. Null is equivalent to
     *            {@link FluidVolume#isEmpty() empty}.
     * @param maxAmount The maximum amount of fluid to extract. Note that the returned {@link FluidVolume} may have an
     *            amount up to this given amount plus the amount in "toAddWith".
     * @return The extracted {@link FluidVolume}, merged with "toAddWith".
     * @deprecated Replaced by
     *             {@link #extractSingle(FixedFluidInv, int, FluidFilter, FluidVolume, FluidAmount, Simulation)} */
    @Deprecated
    public static FluidVolume extractSingle(
        FixedFluidInv inv, int tank, @Nullable FluidFilter filter, FluidVolume toAddWith, int maxAmount,
        Simulation simulation
    ) {
        return extractSingle(inv, tank, filter, toAddWith, FluidAmount.of1620(maxAmount), simulation);
    }

    /** Extracts a single {@link FluidVolume} from a {@link FixedFluidInv}, using only
     * {@link FixedFluidInv#setInvFluid(int, FluidVolume, Simulation)}. As such this is useful for implementations of
     * {@link FluidExtractable} (or others) for their base implementations.
     * 
     * @param filter The filter to match on. If this is null then it matches on anything.
     * @param toAddWith An optional {@link FluidVolume} that the extracted fluid will be added to. Null is equivalent to
     *            {@link FluidVolume#isEmpty() empty}.
     * @param maxAmount The maximum amount of fluid to extract. Note that the returned {@link FluidVolume} may have an
     *            amount up to this given amount plus the amount in "toAddWith".
     * @return The extracted {@link FluidVolume}, merged with "toAddWith". */
    public static FluidVolume extractSingle(
        FixedFluidInv inv, int tank, @Nullable FluidFilter filter, FluidVolume toAddWith, FluidAmount maxAmount,
        Simulation simulation
    ) {
        if (toAddWith == null) {
            toAddWith = EMPTY;
        }

        FluidVolume inTank = inv.getInvFluid(tank);
        if (inTank.isEmpty() || (filter != null && !filter.matches(inTank.fluidKey))) {
            return toAddWith;
        }
        inTank = inTank.copy();
        FluidVolume addable = inTank.split(maxAmount);
        FluidVolume merged = FluidVolume.merge(toAddWith, addable);
        if (merged != null && inv.setInvFluid(tank, inTank, simulation)) {
            toAddWith = merged;
        }
        return toAddWith;
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
