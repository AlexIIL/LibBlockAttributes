/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.compat.mod.vanilla;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeSourceType;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.fluid.FluidContainerRegistry;
import alexiil.mc.lib.attributes.fluid.FluidContainerRegistry.FluidFillHandler;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.GroupedFluidInvFixedWrapper;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.fluid.volume.PotionFluidKey;
import alexiil.mc.lib.attributes.misc.AbstractItemBasedAttribute;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;

/** Compat handler for vanilla. (Obviously the normal rules for compat code doesn't apply here: instead this is just
 * here as a better location than cluttering up any other package). */
public final class VanillaFluidCompat {
    private VanillaFluidCompat() {}

    public static void load() {

        FluidVolume waterVolume = FluidKeys.WATER.withAmount(FluidAmount.BUCKET);
        FluidContainerRegistry.mapContainer(Items.BUCKET, Items.WATER_BUCKET, waterVolume);

        FluidVolume lavaVolume = FluidKeys.LAVA.withAmount(FluidAmount.BUCKET);
        FluidContainerRegistry.mapContainer(Items.BUCKET, Items.LAVA_BUCKET, lavaVolume);

        PotionBottleFillHandler potions = PotionBottleFillHandler.INSTANCE;
        FluidContainerRegistry.registerFillHandler(Items.GLASS_BOTTLE, FluidKeys.WATER, potions);
        FluidContainerRegistry.registerFillHandler(Items.GLASS_BOTTLE, PotionFluidKey.class, false, potions);

        FluidAttributes.forEachGroupedInv(
            attr -> attr.setItemAdder(AttributeSourceType.INSTANCE, Items.POTION, (ref, excess, list) -> {
                list.offer(new PotionItemInv(ref, excess));
            })
        );

        registerCauldronAttributes(Blocks.CAULDRON, Fluids.EMPTY);
        registerCauldronAttributes(Blocks.WATER_CAULDRON, Fluids.WATER);
        registerCauldronAttributes(Blocks.LAVA_CAULDRON, Fluids.LAVA);
    }

    public static void registerCauldronAttributes(Block cauldron, Fluid fluid) {
        registerCauldronAttributes(cauldron, FluidKeys.get(fluid));
    }

    public static void registerCauldronAttributes(Block cauldron, FluidKey fluid) {
        FluidAttributes.forEachInv(
            attr -> attr.setBlockAdder(AttributeSourceType.COMPAT_WRAPPER, cauldron, (w, p, state, list) -> {
                list.offer(new CauldronBlockInv(w, p, cauldron, fluid));
            })
        );
    }

    static final class PotionBottleFillHandler extends FluidFillHandler {
        static final PotionBottleFillHandler INSTANCE = new PotionBottleFillHandler();

        private PotionBottleFillHandler() {
            super(FluidAmount.BOTTLE);
        }

        @Override
        protected FluidVolume insert(
            ItemStack stack, FluidVolume fluid, Simulation simulation, StackReturnFunc stackReturn
        ) {
            Potion potion;
            if (fluid.getFluidKey() == FluidKeys.WATER) {
                potion = Potions.WATER;
            } else {
                potion = ((PotionFluidKey) fluid.getFluidKey()).potion;
            }

            FluidVolume newFluid = fluid.copy();
            ItemStack oldStack = stack;
            stack.decrement(1);
            newFluid.split(FluidAmount.BOTTLE);
            ItemStack newStack = new ItemStack(Items.POTION);
            PotionUtil.setPotion(newStack, potion);
            return stackReturn.returnStacks(oldStack, newStack) ? newFluid : fluid;
        }

        @Override
        protected FluidAmount getCapacity(FluidFilter filter) {
            return FluidAmount.BOTTLE;
        }
    }

    static final class PotionItemInv extends AbstractItemBasedAttribute implements GroupedFluidInv {

        public PotionItemInv(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excessStacks) {
            super(stackRef, excessStacks);
        }

        @Override
        public Set<FluidKey> getStoredFluids() {
            ItemStack stack = stackRef.get();
            if (stack.getItem() != Items.POTION) {
                return Collections.emptySet();
            }
            FluidKey key = FluidKeys.get(PotionUtil.getPotion(stack));
            return key.isEmpty() ? Collections.emptySet() : Collections.singleton(key);
        }

        @Override
        public FluidInvStatistic getStatistics(FluidFilter filter) {
            ItemStack stack = stackRef.get();
            if (stack.getItem() != Items.POTION) {
                return FluidInvStatistic.emptyOf(filter);
            }
            FluidKey key = FluidKeys.get(PotionUtil.getPotion(stack));
            if (key.isEmpty() || !filter.matches(key)) {
                return FluidInvStatistic.emptyOf(filter);
            }
            return new FluidInvStatistic(
                filter, FluidAmount.BOTTLE.mul(stack.getCount()), FluidAmount.ZERO, FluidAmount.ZERO
            );
        }

        @Override
        public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
            return fluid;
        }

        @Override
        public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
            if (maxAmount.isLessThan(FluidAmount.BOTTLE)) {
                return FluidVolumeUtil.EMPTY;
            }
            ItemStack stack = stackRef.get();
            if (stack.getItem() != Items.POTION) {
                return FluidVolumeUtil.EMPTY;
            }
            FluidKey key = FluidKeys.get(PotionUtil.getPotion(stack));
            if (key.isEmpty() || !filter.matches(key)) {
                return FluidVolumeUtil.EMPTY;
            }
            ItemStack oldStack = stack.copy();
            oldStack.decrement(1);
            if (setStacks(simulation, oldStack, new ItemStack(Items.GLASS_BOTTLE))) {
                return key.withAmount(FluidAmount.BOTTLE);
            } else {
                return FluidVolumeUtil.EMPTY;
            }
        }
    }

    static final class CauldronBlockInv implements FixedFluidInv {

        final World world;
        final BlockPos pos;
        final Block cauldronBlock;
        final FluidKey cauldronFluid;

        GroupedFluidInv grouped;

        public CauldronBlockInv(World w, BlockPos p, Block cauldronBlock, FluidKey fluid) {
            this.world = w;
            this.pos = p;
            this.cauldronBlock = cauldronBlock;
            this.cauldronFluid = fluid;
        }

        private boolean isValid(BlockState state) {
            return state.getBlock() == cauldronBlock && state.contains(LeveledCauldronBlock.LEVEL);
        }

        @Override
        public int getTankCount() {
            BlockState state = world.getBlockState(pos);
            return isValid(state) ? 1 : 0;
        }

        @Override
        public FluidVolume getInvFluid(int tank) {
            BlockState state = world.getBlockState(pos);
            if (isValid(state)) {
                int level = state.get(LeveledCauldronBlock.LEVEL);
                return cauldronFluid.withAmount(FluidAmount.of(level, 3));
            }
            return FluidVolumeUtil.EMPTY;
        }

        @Override
        public boolean isFluidValidForTank(int tank, FluidKey fluid) {
            return this.cauldronFluid == fluid;
        }

        @Override
        public FluidFilter getFilterForTank(int tank) {
            return cauldronFluid.exactFilter;
        }

        @Override
        public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
            BlockState state = world.getBlockState(pos);
            if (isValid(state)) {
                int level;
                if (to.amount().isZero()) {
                    level = 0;
                } else if (FluidAmount.ONE.equals(to.amount())) {
                    level = 3;
                } else if (to.amount().whole == 0 && to.amount().denominator == 3) {
                    level = (int) to.amount().numerator;
                } else {
                    return false;
                }

                if (simulation.isAction()) {
                    world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, level));
                }
            }
            return false;
        }

        @Override
        public FluidVolume insertFluid(int tank, FluidVolume volume, Simulation simulation) {
            if (volume.fluidKey != cauldronFluid || volume.isEmpty()) {
                return volume;
            }
            if (volume.amount().isLessThan(FluidAmount.BOTTLE)) {
                return volume;
            }
            BlockState state = world.getBlockState(pos);
            if (!isValid(state)) {
                return volume;
            }
            int current = state.get(LeveledCauldronBlock.LEVEL);
            int space = 3 - current;
            if (space <= 0) {
                return volume;
            }
            int bottles = volume.amount().asInt(3, RoundingMode.DOWN);
            assert bottles >= 1;
            FluidVolume incomingFluid = volume.copy();
            int additional = Math.min(space, bottles);
            incomingFluid.split(FluidAmount.of(additional, 3));
            if (simulation.isAction()) {
                world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, current + additional));
            }
            return incomingFluid;
        }

        @Override
        public FluidVolume extractFluid(
            int tank, FluidFilter filter, FluidVolume mergeWith, FluidAmount maxAmount, Simulation simulation
        ) {
            if (!mergeWith.isEmpty() && cauldronFluid != mergeWith.fluidKey) {
                return mergeWith;
            }
            if (filter != null && !filter.matches(FluidKeys.WATER)) {
                return mergeWith;
            }
            if (maxAmount.isLessThan(FluidAmount.BOTTLE)) {
                return mergeWith;
            }
            BlockState state = world.getBlockState(pos);
            if (!isValid(state)) {
                return mergeWith;
            }
            int current = state.get(LeveledCauldronBlock.LEVEL);
            if (current <= 0) {
                return mergeWith;
            }
            int maxBottles = maxAmount.asInt(3, RoundingMode.DOWN);
            assert maxBottles >= 1;
            int extractedBottles = Math.min(maxBottles, current);
            int left = current - extractedBottles;
            FluidVolume extracted = cauldronFluid.withAmount(FluidAmount.of(extractedBottles, 3));

            if (simulation.isAction()) {
                world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, left));
            }

            if (mergeWith.isEmpty()) {
                return extracted;
            } else {
                mergeWith.merge(extracted, Simulation.ACTION);
                return mergeWith;
            }
        }

        @Override
        public GroupedFluidInv getGroupedInv() {
            if (grouped == null) {
                grouped = new GroupedFluidInvFixedWrapper(this) {
                    @Override
                    public FluidAmount getMinimumAcceptedAmount() {
                        return FluidAmount.BOTTLE;
                    }

                    @Override
                    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
                        return CauldronBlockInv.this.insertFluid(0, fluid, simulation);
                    }
                };
            }
            return grouped;
        }
    }
}
