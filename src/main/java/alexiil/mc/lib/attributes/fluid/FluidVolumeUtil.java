package alexiil.mc.lib.attributes.fluid;

import java.util.function.Consumer;

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
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;
import alexiil.mc.lib.attributes.misc.Ref;

public enum FluidVolumeUtil {
    ;

    private static final FluidVolume EMPTY = FluidKeys.EMPTY.withAmount(0);

    private static final boolean LONG_LOCALISATION = true;
    private static final boolean USE_FULL_NAMES = true;

    /** @param amount The amount in {@link FluidVolume#BASE_UNIT base units} */
    public static String localizeFluidAmount(int amount) {
        if (LONG_LOCALISATION) {
            if (amount < FluidVolume.BASE_UNIT) {
                return "0";
            }
            // TODO: Actual localisation!
            // (I'd like to copy this almost directly from buildcraft's LocaleUtil.localizeFluid)
            if (amount > FluidVolume.BUCKET) {
                return (amount / (double) FluidVolume.BUCKET) + " Buckets";
            }
            if (amount > FluidVolume.INGOT) {
                return (amount / (double) FluidVolume.INGOT) + " Ingots";
            }
            return (amount / (double) FluidVolume.NUGGET) + " Nuggets";
        } else {
            return amount / (double) FluidVolume.BUCKET + "Buckets";
        }
    }

    /** Attempts to move as much fluid as possible from the {@link IFluidExtractable} to the {@link IFluidInsertable}.
     * 
     * @return A copy of the fluid moved.
     * @see #move(IFluidExtractable, IFluidInsertable, IFluidFilter, int) */
    public static FluidVolume move(IFluidExtractable from, IFluidInsertable to) {
        return move(from, to, null, Integer.MAX_VALUE);
    }

    /** Attempts to move up to the given amount of fluid from the {@link IFluidExtractable} to the
     * {@link IFluidInsertable}.
     * 
     * @return A copy of the fluid moved.
     * @see #move(IFluidExtractable, IFluidInsertable, IFluidFilter, int) */
    public static FluidVolume move(IFluidExtractable from, IFluidInsertable to, int maximum) {
        return move(from, to, null, maximum);
    }

    /** Attempts to move up to the given maximum amount of fluids from the {@link IFluidExtractable} to the
     * {@link IFluidInsertable}, provided they match the given {@link IItemFilter}.
     * 
     * @return A copy of the fluid moved. */
    public static FluidVolume move(IFluidExtractable from, IFluidInsertable to, IFluidFilter filter, int maximum) {
        IFluidFilter insertionFilter = to.getInsertionFilter();
        if (filter != null && filter != ConstantFluidFilter.ANYTHING) {
            insertionFilter = AggregateFluidFilter.and(insertionFilter, filter);
        }

        FluidVolume extracted = from.attemptExtraction(insertionFilter, maximum, Simulation.SIMULATE);
        if (extracted.isEmpty()) {
            return EMPTY;
        }
        FluidVolume leftover = to.attemptInsertion(extracted, Simulation.ACTION);
        int insertedAmount = extracted.getAmount() - (leftover.isEmpty() ? 0 : leftover.getAmount());
        FluidVolume reallyExtracted = from.attemptExtraction(insertionFilter, insertedAmount, Simulation.ACTION);

        if (reallyExtracted.isEmpty()) {
            throw throwBadImplException("Tried to extract the filter (C) from A but it returned an empty item stack "
                + "after we have already inserted the expected stack into B!\nThe inventory is now in an invalid (duped) state!",
                new String[] { "from A", "to B", "filter C" }, new Object[] { from, to, filter });
        }
        if (reallyExtracted.getAmount() != insertedAmount) {
            throw throwBadImplException(
                "Tried to extract " + insertedAmount + " but we actually extracted " + reallyExtracted.getAmount()
                    + "!\nThe inventory is now in an invalid (duped) state!",
                new String[] { "from A", "to B", "filter C", "originally extracted", "really extracted" },
                new Object[] { from, to, insertionFilter, extracted, reallyExtracted });
        }
        return extracted;
    }

    /** @return An {@link IFluidInsertable} that will insert fluids into the given stack (overflowing into the given
     *         {@link Consumer}) */
    public static IFluidInsertable createItemInventoryInsertable(Ref<ItemStack> stackRef,
        Consumer<ItemStack> excessStacks) {
        return (FluidVolume fluid, Simulation simulate) -> {
            ItemStack stack = stackRef.obj;
            if (!(stack.getItem() instanceof IFluidItem)) {
                return fluid;
            }
            stack = stack.copy();
            final ItemStack split = stack.getAmount() > 1 ? stack.split(1) : stack;
            IFluidItem fluidItem = (IFluidItem) stack.getItem();
            Ref<ItemStack> single = new Ref<>(split);
            Ref<FluidVolume> incomingFluid = new Ref<>(fluid);
            if (fluidItem.fill(single, incomingFluid)) {
                fluid = incomingFluid.obj;
                if (simulate == Simulation.ACTION) {
                    if (/* If we split the stack */ stack != split) {
                        excessStacks.accept(single.obj);
                    } else {
                        stackRef.obj = single.obj;
                    }
                }
            }
            return fluid;
        };
    }

    public static IFluidExtractable createItemInventoryExtractable(Ref<ItemStack> stackRef,
        Consumer<ItemStack> excessStacks) {
        return (IFluidFilter filter, int maxAmount, Simulation simulate) -> {

            final ItemStack stack = stackRef.obj.copy();
            final ItemStack split = stack.getAmount() > 1 ? stack.split(1) : stack;
            FluidVolume drained = EMPTY;
            if (stack.getItem() instanceof IFluidItem) {
                IFluidItem fluidItem = (IFluidItem) stack.getItem();
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

    public static boolean interactWithTank(IFixedFluidInv inv, PlayerEntity player, Hand hand) {
        ItemStack inHand = player.getStackInHand(hand);
        if (inHand.isEmpty()) {
            return false;
        }
        Ref<ItemStack> stack = new Ref<>(inHand);
        FluidTankInteraction result = interactWithTank(inv, stack, ItemInvUtil.createPlayerInsertable(player));
        if (!result.didMoveAny()) {
            return false;
        }
        player.setStackInHand(hand, stack.obj);
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
        player.playSound(soundEvent, SoundCategory.BLOCK, 1.0f, 1.0f);
        return true;
    }

    /** @param inv The fluid inventory to interact with
     * @param stack The held {@link ItemStack} to interact with.
     * @param excessStacks A {@link Consumer} to take the excess itemstack. */
    public static FluidTankInteraction interactWithTank(IFixedFluidInv inv, Ref<ItemStack> stack,
        Consumer<ItemStack> excessStacks) {
        if (stack.obj.isEmpty() || !(stack.obj.getItem() instanceof IFluidItem)) {
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
