/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.AttributeCombiner;
import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.ItemAttributeList;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.compat.silk.SilkFluidCompat;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilterUtil;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFluidInsertable;
import alexiil.mc.lib.attributes.fluid.impl.CombinedGroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.CombinedGroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFluidExtractable;
import alexiil.mc.lib.attributes.fluid.impl.EmptyGroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.RejectingFluidInsertable;
import alexiil.mc.lib.attributes.fluid.mixin.api.IBucketItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;

public final class FluidAttributes {
    private FluidAttributes() {}

    public static final CombinableAttribute<FixedFluidInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<FixedFluidInv> FIXED_INV;
    public static final CombinableAttribute<GroupedFluidInvView> GROUPED_INV_VIEW;
    public static final CombinableAttribute<GroupedFluidInv> GROUPED_INV;
    public static final CombinableAttribute<FluidInsertable> INSERTABLE;
    public static final CombinableAttribute<FluidExtractable> EXTRACTABLE;

    /** Mostly intended to be used for {@link ItemStack}'s, not {@link Block}'s. (As this interface doesn't really make
     * much sense when applied to block's alone, however it makes much more sense in pipe input or extraction
     * filters). */
    public static final CombinableAttribute<FluidFilter> FILTER;

    static {
        FIXED_INV_VIEW = create(
            FixedFluidInvView.class, //
            EmptyFixedFluidInv.INSTANCE, //
            CombinedFixedFluidInvView::new, //
            inv -> inv
        );
        FIXED_INV = create(
            FixedFluidInv.class, //
            EmptyFixedFluidInv.INSTANCE, //
            CombinedFixedFluidInv::new, //
            Function.identity()
        );
        GROUPED_INV_VIEW = create(
            GroupedFluidInvView.class, //
            EmptyGroupedFluidInv.INSTANCE, //
            list -> new CombinedGroupedFluidInvView(list), //
            FixedFluidInv::getGroupedInv
        );
        GROUPED_INV = create(
            GroupedFluidInv.class, //
            EmptyGroupedFluidInv.INSTANCE, //
            list -> new CombinedGroupedFluidInv(list), //
            FixedFluidInv::getGroupedInv
        );
        INSERTABLE = create(
            FluidInsertable.class, //
            RejectingFluidInsertable.NULL, //
            list -> new CombinedFluidInsertable(list), //
            FixedFluidInv::getInsertable
        );
        EXTRACTABLE = create(
            FluidExtractable.class, //
            EmptyFluidExtractable.NULL, //
            list -> new CombinedFluidExtractable(list), //
            FixedFluidInv::getExtractable
        );

        FILTER = Attributes.createCombinable(
            FluidFilter.class, //
            ConstantFluidFilter.NOTHING, //
            list -> AggregateFluidFilter.allOf(list)//
        );

        try {
            Class.forName("io.github.prospector.silk.fluid.FluidContainerProvider");
            LibBlockAttributes.LOGGER.info("Silk found, loading compatibility for fluids.");
            SilkFluidCompat.load();
        } catch (ClassNotFoundException cnfe) {
            LibBlockAttributes.LOGGER.info("Silk not found, not loading compatibility for fluids.");
        }
    }

    public static <T> CombinableAttribute<T> create(
        Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner, Function<FixedFluidInv, T> convertor
    ) {

        return Attributes.createCombinable(clazz, defaultValue, combiner)//
            .appendItemAdder((stackRef, excess, to) -> appendItemAttributes(convertor, stackRef, excess, to));
    }

    private static <T> void appendItemAttributes(
        Function<FixedFluidInv, T> convertor, Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess,
        ItemAttributeList<T> to
    ) {

        ItemStack stack = stackRef.get();
        Item item = stack.getItem();

        if (item instanceof FishBucketItem) {
            return;
        }

        if (isValidBucket(stack)) {
            to.offer(new BucketItemGroupedFluidInv(stackRef, excess));
            return;
        }
    }

    private static boolean isValidBucket(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof IBucketItem && ((IBucketItem) item).libblockattributes__shouldExposeFluid();
    }

    /** A {@link FixedFluidInv} for a {@link BucketItem}. Package-private because this should always be accessed via the
     * attributes. */
    static final class BucketItemGroupedFluidInv implements GroupedFluidInv {

        private final Reference<ItemStack> stackRef;
        private final LimitedConsumer<ItemStack> excess;

        public BucketItemGroupedFluidInv(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excess) {
            this.stackRef = stackRef;
            this.excess = excess;
        }

        // @Override
        // public int getTankCount() {
        // return 1;
        // }
        //
        // @Override
        // public FluidVolume getInvFluid(int tank) {
        // ItemStack stack = stackRef.get();
        // if (!isValidBucket(stack)) {
        // return FluidVolumeUtil.EMPTY;
        // }
        // IBucketItem bucket = (IBucketItem) stack.getItem();
        // FluidKey fluid = bucket.libblockattributes__getFluid(stack);
        // if (fluid == FluidKeys.EMPTY) {
        // return FluidVolumeUtil.EMPTY;
        // } else {
        // return fluid.withAmount(bucket.libblockattributes__getFluidVolumeAmount() * stack.getCount());
        // }
        // }
        //
        // @Override
        // public int getMaxAmount(int tank) {
        // ItemStack stack = stackRef.get();
        // if (!isValidBucket(stack)) {
        // return 0;
        // }
        // IBucketItem bucket = (IBucketItem) stack.getItem();
        // return stack.getCount() * bucket.libblockattributes__getFluidVolumeAmount();
        // }
        //
        // @Override
        // public boolean isFluidValidForTank(int tank, FluidKey fluid) {
        // ItemStack stack = stackRef.get();
        // if (!isValidBucket(stack)) {
        // return false;
        // }
        // IBucketItem bucket = (IBucketItem) stack.getItem();
        // return !bucket.libblockattributes__withFluid(fluid).isEmpty();
        // }
        //
        // @Override
        // public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
        // return null;
        // }

        @Override
        public Set<FluidKey> getStoredFluids() {
            ItemStack stack = stackRef.get();
            if (!isValidBucket(stack)) {
                return Collections.emptySet();
            }
            IBucketItem bucket = (IBucketItem) stack.getItem();
            FluidKey fluid = bucket.libblockattributes__getFluid(stack);
            if (fluid == FluidKeys.EMPTY) {
                return Collections.emptySet();
            }
            return Collections.singleton(fluid);
        }

        @Override
        public FluidAmount getTotalCapacity_F() {
            ItemStack stack = stackRef.get();
            if (!isValidBucket(stack)) {
                return FluidAmount.ZERO;
            }
            IBucketItem bucket = (IBucketItem) stack.getItem();
            FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
            return perBucket.mul(stack.getCount());
        }

        @Override
        public FluidInvStatistic getStatistics(FluidFilter filter) {
            ItemStack stack = stackRef.get();
            if (!isValidBucket(stack)) {
                return FluidInvStatistic.emptyOf(filter);
            }

            IBucketItem bucket = (IBucketItem) stack.getItem();
            FluidKey current = bucket.libblockattributes__getFluid(stack);

            if (current != FluidKeys.EMPTY) {
                if (filter.matches(current)) {
                    FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
                    FluidAmount amount = perBucket.mul(stack.getCount());
                    return new FluidInvStatistic(filter, amount, FluidAmount.ZERO, amount);
                } else {
                    return FluidInvStatistic.emptyOf(filter);
                }
            }

            Set<FluidKey> any = FluidFilterUtil.decomposeFilter(filter);

            if (any != null) {
                FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
                FluidAmount space = perBucket.mul(stack.getCount());
                for (FluidKey key : any) {
                    if (!bucket.libblockattributes__withFluid(key).isEmpty()) {
                        return new FluidInvStatistic(filter, FluidAmount.ZERO, FluidAmount.ZERO, space);
                    }
                }
            }

            return FluidInvStatistic.emptyOf(filter);
        }

        @Override
        public ListenerToken addListener(FluidInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
            return null;
        }

        @Override
        public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
            ItemStack stack = stackRef.get();
            if (!isValidBucket(stack)) {
                return fluid;
            }
            IBucketItem bucket = (IBucketItem) stack.getItem();
            FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
            if (fluid.getAmount_F().isLessThan(perBucket)) {
                return fluid;
            }
            FluidKey current = bucket.libblockattributes__getFluid(stack);
            if (!current.isEmpty()) {
                return fluid;
            }
            ItemStack newStack = bucket.libblockattributes__withFluid(fluid.fluidKey);
            if (newStack.isEmpty()) {
                return fluid;
            }

            stack = stack.copy();
            stack.decrement(1);

            fluid = fluid.copy();
            FluidVolume splitOff = fluid.split(perBucket);
            if (!splitOff.getAmount_F().equals(perBucket)) {
                throw new IllegalStateException(
                    "Split off amount was not equal to perBucket!"//
                        + "\n\tsplitOff = " + splitOff//
                        + "\n\tfluid = " + fluid//
                        + "\n\tperBucket = " + perBucket//
                );
            }

            if (stack.isEmpty()) {
                if (!stackRef.isValid(newStack)) {
                    if (!stackRef.isValid(ItemStack.EMPTY) || !excess.wouldAccept(newStack)) {
                        return fluid;
                    }
                }
            } else {
                if (!stackRef.isValid(stack) || !excess.wouldAccept(newStack)) {
                    return fluid;
                }
            }

            setStacks(simulation, stack, newStack);

            return fluid;
        }

        @Override
        public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
            ItemStack stack = stackRef.get();
            if (!isValidBucket(stack)) {
                return FluidVolumeUtil.EMPTY;
            }
            IBucketItem bucket = (IBucketItem) stack.getItem();
            FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
            if (maxAmount.isLessThan(perBucket)) {
                return FluidVolumeUtil.EMPTY;
            }
            FluidKey current = bucket.libblockattributes__getFluid(stack);
            if (current.isEmpty() || !filter.matches(current)) {
                return FluidVolumeUtil.EMPTY;
            }

            ItemStack newStack = bucket.libblockattributes__drainedOfFluid(stack);

            stack = stack.copy();
            stack.decrement(1);

            if (stack.isEmpty()) {
                if (!stackRef.isValid(newStack)) {
                    if (!stackRef.isValid(ItemStack.EMPTY) || !excess.wouldAccept(newStack)) {
                        return FluidVolumeUtil.EMPTY;
                    }
                }
            } else {
                if (!stackRef.isValid(stack) || !excess.wouldAccept(newStack)) {
                    return FluidVolumeUtil.EMPTY;
                }
            }

            setStacks(simulation, stack, newStack);

            return current.withAmount(perBucket);
        }

        private void setStacks(Simulation simulation, ItemStack stack, ItemStack newStack) {
            if (simulation == Simulation.ACTION) {
                if (stack.isEmpty()) {
                    if (!stackRef.set(newStack)) {
                        boolean s0 = stackRef.set(ItemStack.EMPTY);
                        boolean s1 = excess.offer(newStack);
                        if (!s0 | !s1) {
                            throw new IllegalStateException(
                                "Failed to set the stack/append items to the excess! (Even though we just checked this up above...)"
                                    + "\n\tstackRef = " + stackRef//
                                    + "\n\texcess = " + excess//
                                    + "\n\tnewStack = " + newStack//
                                    + "\n\tstatus = " + s0 + " " + s1
                            );
                        }
                    }
                } else {
                    boolean s0 = stackRef.set(stack);
                    boolean s1 = excess.offer(newStack);
                    if (!s0 | !s1) {
                        throw new IllegalStateException(
                            "Failed to set the stack/append items to the excess! (Even though we just checked this up above...)"
                                + "\n\tstackRef = " + stackRef//
                                + "\n\texcess = " + excess//
                                + "\n\toldStack = " + stack//
                                + "\n\tnewStack = " + newStack//
                                + "\n\tstatus = " + s0 + " " + s1
                        );
                    }
                }
            }
        }
    }
}
