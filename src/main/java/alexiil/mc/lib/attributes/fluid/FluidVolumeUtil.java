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
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
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
        return move(from, to, null, FluidAmount.MAX_BUCKETS);
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
    public static FluidVolume move(FluidExtractable from, FluidInsertable to, FluidFilter filter) {
        return move(from, to, filter, null);
    }

    /** Attempts to move up to the given maximum amount of fluids from the {@link FluidExtractable} to the
     * {@link FluidInsertable}, provided they match the given {@link FluidFilter}.
     * 
     * @return A copy of the fluid moved. */
    public static FluidVolume move(
        FluidExtractable from, FluidInsertable to, @Nullable FluidFilter filter, @Nullable FluidAmount maximum
    ) {
        FluidFilter insertionFilter = to.getInsertionFilter();
        if (filter != null && filter != ConstantFluidFilter.ANYTHING) {
            insertionFilter = AggregateFluidFilter.and(insertionFilter, filter);
        }
        if (maximum == null) {
            maximum = FluidAmount.MAX_BUCKETS;
        }

        // 5 steps:
        // 1: (Simulate) Try to extract as much as possible, to find out the maximum amount of fluid available
        // 2: (Simulate) Try to insert as much of the extracted as possible
        // 3: (Simulate) Try to extract the exact amount that was actually inserted
        /* We don't need to simulate inserting the exact amount because it should always be safe to insert the amount
         * minus the result. */
        // If all of the above steps provide an exact amount > 0:
        // 4: Extract the exact amount
        // 5: Insert the exact fluid.
        // and assert that there is no result.

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
            new String[] { "inserted A", "result B", "inserted C", "result D", "insertable E" },
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

    /** @return An {@link FluidExtractable} that will extract fluids from the given stack (overflowing into the given
     *         {@link Consumer})
     * @deprecated This has been replaced by the item-based attributes system. */
    @Deprecated
    public static FluidExtractable createItemInventoryExtractable(
        Ref<ItemStack> stackRef, Consumer<ItemStack> excessStacks
    ) {
        return FluidAttributes.EXTRACTABLE.get(stackRef, LimitedConsumer.fromConsumer(excessStacks));
    }

    // #####################################
    // Various methods moved to FluidInvUtil
    // #####################################

    /** @deprecated The boolean return has been deprecated, and the main method has been moved to
     *             {@link FluidInvUtil#interactHandWithTank(FixedFluidInv, PlayerEntity, Hand)} */
    @Deprecated
    public static boolean interactWithTank(FixedFluidInv inv, PlayerEntity player, Hand hand) {
        return FluidInvUtil.interactHandWithTank(inv, player, hand).didMoveAny();
    }

    /** @deprecated The boolean return has been deprecated, and the main method has been moved to
     *             {@link FluidInvUtil#interactHandWithTank(FluidTransferable, PlayerEntity, Hand)} */
    @Deprecated
    public static boolean interactWithTank(FluidTransferable inv, PlayerEntity player, Hand hand) {
        return FluidInvUtil.interactHandWithTank(inv, player, hand).didMoveAny();
    }

    /** @deprecated The boolean return has been deprecated, and the main method has been moved to
     *             {@link FluidInvUtil#interactHandWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Hand)} */
    @Deprecated
    public static boolean interactWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player, Hand hand
    ) {
        return FluidInvUtil.interactHandWithTank(invInsert, invExtract, player, hand).didMoveAny();
    }

    /** @deprecated The boolean return has been deprecated, and the main method has been moved to
     *             {@link FluidInvUtil#interactCursorWithTank(FixedFluidInv, ServerPlayerEntity)} */
    @Deprecated
    public static boolean interactCursorWithTank(FixedFluidInv inv, ServerPlayerEntity player) {
        return FluidInvUtil.interactCursorWithTank(inv, player).didMoveAny();
    }

    /** @deprecated The boolean return has been deprecated, and the main method has been moved to
     *             {@link FluidInvUtil#interactCursorWithTank(FluidTransferable, ServerPlayerEntity)} */
    @Deprecated
    public static boolean interactCursorWithTank(FluidTransferable inv, ServerPlayerEntity player) {
        return FluidInvUtil.interactCursorWithTank(inv, player).didMoveAny();
    }

    /** @deprecated The boolean return has been deprecated, and the main method has been moved to
     *             {@link FluidInvUtil#interactCursorWithTank(FluidInsertable, FluidExtractable, ServerPlayerEntity)} */
    @Deprecated
    public static boolean interactCursorWithTank(
        FluidInsertable invInsert, FluidExtractable invExtract, ServerPlayerEntity player
    ) {
        return FluidInvUtil.interactCursorWithTank(invInsert, invExtract, player).didMoveAny();
    }

    /** @deprecated The boolean return has been deprecated, and the main method has been moved to
     *             {@link FluidInvUtil#interactWithTank(FluidInsertable, FluidExtractable, PlayerEntity, Reference)} */
    @Deprecated
    public static boolean interactWithTank(
        @Nullable FluidInsertable invInsert, @Nullable FluidExtractable invExtract, PlayerEntity player,
        Reference<ItemStack> mainStackRef
    ) {
        return FluidInvUtil.interactWithTank(invInsert, invExtract, player, mainStackRef).didMoveAny();
    }

    /** @deprecated This has been replaced by
     *             {@link FluidInvUtil#interactItemWithTank(FixedFluidInv, Reference, LimitedConsumer)}. */
    @Deprecated
    public static FluidTankInteraction interactWithTank(
        FixedFluidInv inv, Ref<ItemStack> stack, Consumer<ItemStack> excessStacks
    ) {
        return interactWithTank(inv.getInsertable(), inv.getExtractable(), stack, excessStacks);
    }

    /** @deprecated This has been replaced by
     *             {@link FluidInvUtil#interactItemWithTank(FluidTransferable, Reference, LimitedConsumer)}. */
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
     * @deprecated This has been replaced by
     *             {@link FluidInvUtil#interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer)}. */
    public static FluidTankInteraction interactWithTank(
        FluidInsertable invInsert, FluidExtractable invExtract, Ref<ItemStack> stack, Consumer<ItemStack> excessStacks
    ) {
        return FluidInvUtil
            .interactItemWithTank(invInsert, invExtract, stack, LimitedConsumer.fromConsumer(excessStacks));
    }

    /** @deprecated Use {@link FluidInvUtil#interactItemWithTank(FixedFluidInv, Reference, LimitedConsumer)} instead:
     *             all interactWithTank methods have been moved to {@link FluidInvUtil}. */
    @Deprecated
    public static FluidTankInteraction interactWithTank(
        FixedFluidInv inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks
    ) {
        return FluidInvUtil.interactItemWithTank(inv.getInsertable(), inv.getExtractable(), stack, excessStacks);
    }

    /** @deprecated Use {@link FluidInvUtil#interactItemWithTank(FluidTransferable, Reference, LimitedConsumer)}
     *             instead: all interactWithTank methods have been moved to {@link FluidInvUtil}. */
    @Deprecated
    public static FluidTankInteraction interactWithTank(
        FluidTransferable inv, Reference<ItemStack> stack, LimitedConsumer<ItemStack> excessStacks
    ) {
        return FluidInvUtil.interactItemWithTank(inv, inv, stack, excessStacks);
    }

    /** @deprecated Use
     *             {@link FluidInvUtil#interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer)}
     *             instead: all interactWithTank methods have been moved to {@link FluidInvUtil}. */
    @Deprecated
    public static FluidTankInteraction interactWithTank(
        FluidInsertable invInsert, FluidExtractable invExtract, Reference<ItemStack> stack,
        LimitedConsumer<ItemStack> excessStacks
    ) {
        return FluidInvUtil.interactItemWithTank(invInsert, invExtract, stack, excessStacks);
    }

    // ############################
    // Normal, non-deprecated stuff
    // ############################

    public static final class FluidTankInteraction {
        public static final FluidTankInteraction NONE = new FluidTankInteraction(EMPTY, false);

        /** A copy of the fluid moved. */
        public final FluidVolume fluidMoved;

        /** If true then the interaction drained fluid from the {@link ItemStack}, and inserted it into the
         * {@link FluidInsertable} tank. Otherwise this will be false. */
        public final boolean intoTank;

        /** The {@link ItemContainerStatus status} of the {@link FluidExtractable} obtained from the {@link ItemStack},
         * via */
        public final ItemContainerStatus intoTankStatus;
        public final ItemContainerStatus fromTankStatus;

        @Deprecated
        public static FluidTankInteraction intoTank(FluidVolume fluid) {
            return new FluidTankInteraction(fluid, true);
        }

        @Deprecated
        public static FluidTankInteraction fromTank(FluidVolume fluid) {
            return new FluidTankInteraction(fluid, false);
        }

        public static FluidTankInteraction none(
            ItemContainerStatus intoTankStatus, ItemContainerStatus fromTankStatus
        ) {
            return new FluidTankInteraction(EMPTY, false, intoTankStatus, fromTankStatus);
        }

        @Deprecated
        public FluidTankInteraction(FluidVolume fluidMoved, boolean intoTank) {
            this.fluidMoved = fluidMoved;
            this.intoTank = intoTank;
            this.intoTankStatus = ItemContainerStatus.NOT_CHECKED;
            this.fromTankStatus = ItemContainerStatus.NOT_CHECKED;
        }

        /** Constructs a new {@link FluidTankInteraction} object.
         * <p>
         * Generally it is not expected that this be called by any method other than
         * {@link FluidInvUtil#interactItemWithTank(FluidInsertable, FluidExtractable, Reference, LimitedConsumer, FluidAmount)} */
        public FluidTankInteraction(
            FluidVolume fluidMoved, boolean intoTank, ItemContainerStatus intoTankStatus,
            ItemContainerStatus fromTankStatus
        ) {
            this.fluidMoved = fluidMoved;
            this.intoTank = intoTank;
            this.intoTankStatus = intoTankStatus == null ? ItemContainerStatus.NOT_CHECKED : intoTankStatus;
            this.fromTankStatus = fromTankStatus == null ? ItemContainerStatus.NOT_CHECKED : fromTankStatus;
        }

        /** Checks to see if any fluid was moved in the interaction.
         * 
         * @return The inverse of {@link #fluidMoved}.{@link FluidVolume#isEmpty() isEmpty()} */
        public boolean didMoveAny() {
            return !fluidMoved.isEmpty();
        }

        /** @return true if either of {@link #intoTankStatus} or {@link #fromTankStatus} is
         *         {@link ItemContainerStatus #VALID}. */
        public boolean wasContainerValid() {
            return intoTankStatus == ItemContainerStatus.VALID || fromTankStatus == ItemContainerStatus.VALID;
        }

        /** @return true if either of {@link #intoTankStatus} or {@link #fromTankStatus} is different to
         *         {@link ItemContainerStatus#NOT_CHECKED}. */
        public boolean didCheckItemStack() {
            return intoTankStatus != ItemContainerStatus.NOT_CHECKED
            || fromTankStatus != ItemContainerStatus.NOT_CHECKED;
        }

        /** Converts this interaction result into a vanilla minecraft {@link ActionResult}, suitable for normal block or
         * item "use" methods.
         * 
         * @return
         *         <ol>
         *         <li>{@link ActionResult#SUCCESS} if {@link #didMoveAny()} returns true.</li>
         *         <li>{@link ActionResult#FAIL} if {@link #wasContainerValid()} returns true.</li>
         *         <li>{@link ActionResult#PASS} otherwise.</li>
         *         </ol>
         *         (This is based on the principle that attempting to use an empty bucket on an empty tank should return
         *         {@link ActionResult#FAIL}, but using an unrelated item - such as an iron ingot - should return
         *         {@link ActionResult#PASS}) */
        public ActionResult asActionResult() {
            if (didMoveAny()) {
                return ActionResult.SUCCESS;
            }
            return wasContainerValid() ? ActionResult.FAIL : ActionResult.PASS;
        }

        @Deprecated
        public int amountMoved() {
            return fluidMoved.getAmount();
        }

        public FluidAmount amountMoved_F() {
            return fluidMoved.getAmount_F();
        }
    }

    public enum ItemContainerStatus {
        /** Indicates that the given {@link ItemStack} cannot have fluid inserted/extracted to/from it. */
        INVALID,

        /** Indicates that we didn't check to see if the given {@link ItemStack} could have fluid inserted/extracted
         * to/from it. This is only returned if the operation didn't need to be performed, either because the previous
         * operation succeeded, or the tank passed didn't have an extractable/insertable object. */
        NOT_CHECKED,

        /** Indicates that the given {@link ItemStack} could have fluid inserted/extracted to/from it. */
        VALID;
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
        FluidTransferResult result = computeInsertion(inv.getInvFluid(tank), inv.getMaxAmount_F(tank), toInsert);
        if (result.result == toInsert) {
            return toInsert;
        }

        if (inv.setInvFluid(tank, result.inTank, simulation)) {
            return result.result;
        } else {
            return toInsert;
        }
    }

    /** Computes the result of {@link #insertSingle(FixedFluidInv, int, FluidVolume, Simulation)}, but without actually
     * modifying an inventory.
     * 
     * @return The result of this insertion. If the insertion failed then the fields will be identical (==) to their
     *         respective {@link FluidVolume}s passed in. */
    public static FluidTransferResult computeInsertion(
        FluidVolume current, FluidAmount capacity, FluidVolume toInsert
    ) {
        if (toInsert.isEmpty()) {
            return new FluidTransferResult(toInsert, current);
        }
        FluidAmount currentA = current.getAmount_F();
        FluidAmount max = currentA.roundedAdd(toInsert.getAmount_F(), RoundingMode.DOWN).min(capacity);
        FluidAmount addable = max.roundedSub(currentA, RoundingMode.UP);
        if (!addable.isPositive()) {
            return new FluidTransferResult(toInsert, current);
        }
        if (currentA.isPositive() && !current.canMerge(toInsert)) {
            return new FluidTransferResult(toInsert, current);
        }
        current = current.copy();
        FluidVolume insertCopy = toInsert.copy();
        FluidVolume merged = FluidVolume.merge(current, insertCopy.split(addable));
        if (merged == null) {
            return new FluidTransferResult(toInsert, current);
        }
        return new FluidTransferResult(insertCopy, merged);
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
     * @return The extracted {@link FluidVolume}, inTank with "toAddWith".
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
     * @return The extracted {@link FluidVolume}, inTank with "toAddWith". */
    public static FluidVolume extractSingle(
        FixedFluidInv inv, int tank, @Nullable FluidFilter filter, FluidVolume toAddWith, FluidAmount maxAmount,
        Simulation simulation
    ) {
        if (toAddWith == null) {
            toAddWith = EMPTY;
        }

        FluidVolume inTank = inv.getInvFluid(tank);
        FluidTransferResult result = computeExtraction(inTank, filter, toAddWith, maxAmount);
        if (inv.setInvFluid(tank, result.inTank, simulation)) {
            return result.result;
        } else {
            return toAddWith;
        }
    }

    /** Computes the result of
     * {@link #extractSingle(FixedFluidInv, int, FluidFilter, FluidVolume, FluidAmount, Simulation)}, but without
     * actually modifying an inventory.
     * 
     * @return The result of this extraction. If the extraction failed then {@link FluidTransferResult#inTank} will be
     *         identical (==) to the passed in {@link FluidVolume}. */
    public static FluidTransferResult computeExtraction(FluidVolume inTank, FluidFilter filter, FluidAmount maxAmount) {
        return computeExtraction(inTank, filter, EMPTY, maxAmount);
    }

    /** Computes the result of
     * {@link #extractSingle(FixedFluidInv, int, FluidFilter, FluidVolume, FluidAmount, Simulation)}, but without
     * actually modifying an inventory.
     * 
     * @return The result of this extraction. If the insertion extraction then the fields will be identical (==) to
     *         their respective {@link FluidVolume}s passed in. */
    public static FluidTransferResult computeExtraction(
        FluidVolume inTank, FluidFilter filter, FluidVolume toAddWith, FluidAmount maxAmount
    ) {
        if (inTank.isEmpty() || (filter != null && !filter.matches(inTank.fluidKey))) {
            return new FluidTransferResult(toAddWith, inTank);
        }
        inTank = inTank.copy();
        FluidVolume addable = inTank.split(maxAmount);
        if (addable.isEmpty()) {
            return new FluidTransferResult(toAddWith, inTank);
        }
        if (!toAddWith.isEmpty()) {
            toAddWith = toAddWith.copy();
        }
        FluidVolume merged = FluidVolume.merge(toAddWith, addable);
        if (merged != null) {
            toAddWith = merged;
        }
        return new FluidTransferResult(toAddWith, inTank);
    }

    public static final class FluidTransferResult {
        /** If this is returned from {@link FluidVolumeUtil#computeInsertion(FluidVolume, FluidAmount, FluidVolume)}
         * then this is the leftover. Otherwise this is the fluid that was extracted from the tank. */
        public final FluidVolume result;

        /** The {@link FluidVolume} that should be placed into the tank */
        public final FluidVolume inTank;

        public FluidTransferResult(FluidVolume result, FluidVolume inTank) {
            this.result = result;
            this.inTank = inTank;
        }
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
