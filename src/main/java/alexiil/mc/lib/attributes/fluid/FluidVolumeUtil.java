/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.Ref;

public enum FluidVolumeUtil {
    ;

    private static final FluidVolume EMPTY = FluidKeys.EMPTY.withAmount(0);

    /** Attempts to move as much fluid as possible from the {@link FluidExtractable} to the {@link FluidInsertable}.
     * 
     * @return A copy of the fluid moved.
     * @see #move(FluidExtractable, FluidInsertable, FluidFilter, int) */
    public static FluidVolume move(FluidExtractable from, FluidInsertable to) {
        return move(from, to, null, Integer.MAX_VALUE);
    }

    /** Attempts to move up to the given amount of fluid from the {@link FluidExtractable} to the
     * {@link FluidInsertable}.
     * 
     * @return A copy of the fluid moved.
     * @see #move(FluidExtractable, FluidInsertable, FluidFilter, int) */
    public static FluidVolume move(FluidExtractable from, FluidInsertable to, int maximum) {
        return move(from, to, null, maximum);
    }

    /** Attempts to move up to the given maximum amount of fluids from the {@link FluidExtractable} to the
     * {@link FluidInsertable}, provided they match the given {@link ItemFilter}.
     * 
     * @return A copy of the fluid moved. */
    public static FluidVolume move(FluidExtractable from, FluidInsertable to, FluidFilter filter, int maximum) {
        FluidFilter insertionFilter = to.getInsertionFilter();
        if (filter != null && filter != ConstantFluidFilter.ANYTHING) {
            insertionFilter = AggregateFluidFilter.and(insertionFilter, filter);
        }

        FluidVolume extracted = from.attemptExtraction(insertionFilter, maximum, Simulation.SIMULATE);
        if (extracted.isEmpty()) {
            return EMPTY;
        }
        FluidVolume leftover = to.attemptInsertion(extracted, Simulation.ACTION);
        int insertedAmount = extracted.getAmount() - leftover.getAmount();
        if (insertedAmount <= 0) {
            return EMPTY;
        }
        ExactFluidFilter exactFilter = new ExactFluidFilter(extracted.fluidKey);
        FluidVolume reallyExtracted = from.attemptExtraction(exactFilter, insertedAmount, Simulation.ACTION);

        if (reallyExtracted.isEmpty()) {
            throw throwBadImplException(
                "Tried to extract the filter (C) from A but it returned an empty item stack "
                    + "after we have already inserted the expected stack into B!\nThe inventory is now in an invalid (duped) state!",
                new String[] { "from A", "to B", "filter C" }, new Object[] { from, to, filter }
            );
        }
        if (reallyExtracted.getAmount() != insertedAmount) {
            throw throwBadImplException(
                "Tried to extract " + insertedAmount + " but we actually extracted " + reallyExtracted.getAmount()
                    + "!\nThe inventory is now in an invalid (duped) state!", new String[] { "from A", "to B",
                        "filter C", "originally extracted", "really extracted" }, new Object[] { from, to,
                            insertionFilter, extracted, reallyExtracted }
            );
        }
        return extracted;
    }

    /** @return An {@link FluidInsertable} that will insert fluids into the given stack (overflowing into the given
     *         {@link Consumer}) */
    public static FluidInsertable createItemInventoryInsertable(Ref<ItemStack> stackRef, Consumer<
        ItemStack> excessStacks) {
        return (FluidVolume fluid, Simulation simulate) -> {
            ItemStack stack = stackRef.obj;
            if (!(stack.getItem() instanceof FluidProviderItem)) {
                return fluid;
            }
            stack = stack.copy();
            final ItemStack split = stack.getAmount() > 1 ? stack.split(1) : stack;
            FluidProviderItem fluidItem = (FluidProviderItem) stack.getItem();
            Ref<ItemStack> filledStackRef = new Ref<>(split);
            Ref<FluidVolume> incomingFluid = new Ref<>(fluid.copy());
            if (fluidItem.fill(filledStackRef, incomingFluid)) {
                fluid = incomingFluid.obj;
                if (simulate == Simulation.ACTION) {
                    if (/* If we split the stack */ stack != split) {
                        excessStacks.accept(filledStackRef.obj);
                        stackRef.obj = stack;
                    } else {
                        stackRef.obj = filledStackRef.obj;
                    }
                }
            }
            return fluid;
        };
    }

    public static FluidExtractable createItemInventoryExtractable(Ref<ItemStack> stackRef, Consumer<
        ItemStack> excessStacks) {
        return (FluidFilter filter, int maxAmount, Simulation simulate) -> {

            final ItemStack stack = stackRef.obj.copy();
            final ItemStack split = stack.getAmount() > 1 ? stack.split(1) : stack;
            FluidVolume drained = EMPTY;
            if (stack.getItem() instanceof FluidProviderItem) {
                FluidProviderItem fluidItem = (FluidProviderItem) stack.getItem();
                Ref<ItemStack> drainedStackRef = new Ref<>(split);
                drained = fluidItem.drain(drainedStackRef);
                if (drained.getAmount() > maxAmount) {
                    return EMPTY;
                }
                if (!drained.isEmpty() && simulate == Simulation.ACTION) {
                    if (/* If we split the stack */ stack != split) {
                        excessStacks.accept(drainedStackRef.obj);
                        stackRef.obj = stack;
                    } else {
                        stackRef.obj = drainedStackRef.obj;
                    }
                }
            }
            return drained;
        };
    }

    public static boolean interactWithTank(FixedFluidInv inv, PlayerEntity player, Hand hand) {
        ItemStack inHand = player.getStackInHand(hand);
        if (inHand.isEmpty()) {
            return false;
        }
        Ref<ItemStack> stack = new Ref<>(inHand);
        boolean isSurvival = !player.abilities.creativeMode;
        Consumer<ItemStack> stackConsumer = isSurvival ? ItemInvUtil.createPlayerInsertable(player) : s -> {};
        FluidTankInteraction result = interactWithTank(inv, stack, stackConsumer);
        if (!result.didMoveAny()) {
            return false;
        }
        if (isSurvival) {
            player.setStackInHand(hand, stack.obj);
        }
        final SoundEvent soundEvent;
        if (result.fluidMoved.fluidKey == FluidKeys.LAVA) {
            soundEvent = result.intoTank ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_FILL_LAVA;
        } else {
            boolean isBottle = inHand.getItem() instanceof GlassBottleItem || inHand.getItem() instanceof PotionItem;
            if (isBottle) {
                soundEvent = result.intoTank ? SoundEvents.ITEM_BOTTLE_EMPTY : SoundEvents.ITEM_BOTTLE_FILL;
            } else {
                soundEvent = result.intoTank ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_FILL;
            }
        }
        player.playSound(soundEvent, SoundCategory.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    /** @param inv The fluid inventory to interact with
     * @param stack The held {@link ItemStack} to interact with.
     * @param excessStacks A {@link Consumer} to take the excess {@link ItemStack}'s. */
    public static FluidTankInteraction interactWithTank(FixedFluidInv inv, Ref<ItemStack> stack, Consumer<
        ItemStack> excessStacks) {
        if (stack.obj.isEmpty() || !(stack.obj.getItem() instanceof FluidProviderItem)) {
            return FluidTankInteraction.NONE;
        }
        FluidVolume fluidMoved = move(inv.getExtractable(), createItemInventoryInsertable(stack, excessStacks));
        if (!fluidMoved.isEmpty()) {
            return FluidTankInteraction.fromTank(fluidMoved);
        }
        fluidMoved = move(createItemInventoryExtractable(stack, excessStacks), inv.getInsertable());
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

        public int amountMoved() {
            return fluidMoved.getAmount();
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
        int current = inTank.getAmount();
        int max = Math.min(current + toInsert.getAmount(), inv.getMaxAmount(tank));
        int addable = max - current;
        if (addable <= 0) {
            return toInsert;
        }
        if (current > 0 && !inTank.canMerge(toInsert)) {
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
     * @return The extracted {@link FluidVolume}, merged with "toAddWith". */
    public static FluidVolume extractSingle(FixedFluidInv inv, int tank, @Nullable FluidFilter filter,
        FluidVolume toAddWith, int maxAmount, Simulation simulation) {

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
