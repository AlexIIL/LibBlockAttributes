/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.block.Block;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.AttributeCombiner;
import alexiil.mc.lib.attributes.AttributeSourceType;
import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fatjar.FatJarChecker;
import alexiil.mc.lib.attributes.fluid.FluidAttributes.BucketItemGroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.compat.mod.LbaFluidModCompatLoader;
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
import alexiil.mc.lib.attributes.misc.AbstractItemBasedAttribute;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes.LbaModule;
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

    /** A {@link List} of every inventory-type attribute, so: {@link #FIXED_INV_VIEW}, {@link #FIXED_INV},
     * {@link #GROUPED_INV_VIEW}, {@link #GROUPED_INV}, {@link #INSERTABLE}, and {@link #EXTRACTABLE}. */
    public static final List<CombinableAttribute<?>> INVENTORY_BASED;

    /** Runs the given {@link Consumer} on every {@link #INVENTORY_BASED} attribute. */
    public static void forEachInv(Consumer<? super CombinableAttribute<?>> consumer) {
        INVENTORY_BASED.forEach(consumer);
    }

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

        INVENTORY_BASED = Arrays.asList(
            FIXED_INV_VIEW, FIXED_INV, //
            GROUPED_INV_VIEW, GROUPED_INV, //
            INSERTABLE, EXTRACTABLE//
        );

        LbaFluidModCompatLoader.load();
    }

    private static <T> CombinableAttribute<T> create(
        Class<T> clazz, @Nonnull T defaultValue, AttributeCombiner<T> combiner, Function<FixedFluidInv, T> convertor
    ) {
        CombinableAttribute<T> attribute = Attributes.createCombinable(clazz, defaultValue, combiner);
        AttributeSourceType srcType = AttributeSourceType.COMPAT_WRAPPER;
        attribute.addItemPredicateAdder(srcType, true, FluidAttributes::isValidBucket, (ref, excess, list) -> {
            list.offer(new BucketItemGroupedFluidInv(ref, excess));
        });
        return attribute;
    }

    private static boolean isValidBucket(ItemStack stack) {
        return isValidBucket(stack.getItem());
    }

    private static boolean isValidBucket(Item item) {
        if (item instanceof FishBucketItem) {
            return false;
        }
        return item instanceof IBucketItem && ((IBucketItem) item).libblockattributes__shouldExposeFluid();
    }

    /** A {@link GroupedFluidInv} for a {@link BucketItem}. Package-private because this should always be accessed via
     * the attributes. */
    static final class BucketItemGroupedFluidInv extends AbstractItemBasedAttribute implements GroupedFluidInv {

        BucketItemGroupedFluidInv(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excessStacks) {
            super(stackRef, excessStacks);
        }

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
            return perBucket.checkedMul(stack.getCount());
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
                    FluidAmount amount = perBucket.checkedMul(stack.getCount());
                    return new FluidInvStatistic(filter, amount, FluidAmount.ZERO, amount);
                } else {
                    return FluidInvStatistic.emptyOf(filter);
                }
            }

            Set<FluidKey> any = FluidFilterUtil.decomposeFilter(filter);

            if (any != null) {
                FluidAmount perBucket = bucket.libblockattributes__getFluidVolumeAmount();
                FluidAmount space = perBucket.checkedMul(stack.getCount());
                for (FluidKey key : any) {
                    if (!bucket.libblockattributes__withFluid(key).isEmpty()) {
                        return new FluidInvStatistic(filter, FluidAmount.ZERO, FluidAmount.ZERO, space);
                    }
                }
            }

            return FluidInvStatistic.emptyOf(filter);
        }

        @Override
        public ListenerToken addListener_F(FluidInvAmountChangeListener_F listener, ListenerRemovalToken removalToken) {
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

            FluidVolume originalFluid = fluid;
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

            return setStacks(simulation, stack, newStack) ? fluid : originalFluid;
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

            if (setStacks(simulation, stack, newStack)) {
                return current.withAmount(perBucket);
            } else {
                return FluidVolumeUtil.EMPTY;
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
        ModContainer fluidsModule = LbaModule.FLUIDS.getModContainer();

        if (fluidsModule == null || coreModule == null) {
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

        Class<?> fluidsClass = FluidAttributes.class;
        Class<?> coreClass = Attribute.class;
        URL fluidsLoc = fluidsClass.getProtectionDomain().getCodeSource().getLocation();
        URL coreLoc = coreClass.getProtectionDomain().getCodeSource().getLocation();

        if (fluidsLoc.equals(coreLoc)) {
            // 9
            throw new Error("(core and fluids have the same path " + fluidsLoc + ")" + FatJarChecker.FATJAR_ERROR);
        }

        // 7
        return;
    }
}
