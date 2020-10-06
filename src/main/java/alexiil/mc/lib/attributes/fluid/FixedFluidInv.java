/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.util.Iterator;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil.FluidTransferResult;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.GroupedFluidInvFixedWrapper;
import alexiil.mc.lib.attributes.fluid.impl.MappedFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.SimpleLimitedFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.SubFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.item.ItemExtractable;

/** A {@link FixedFluidInvView} that can have it's contents changed. Note that this does not imply that the contents can
 * be changed to anything the caller wishes them to be.
 * <p>
 * The attribute is stored in {@link FluidAttributes#FIXED_INV}.
 * <p>
 */
public interface FixedFluidInv extends FixedFluidInvView {

    /** {@inheritDoc}
     * <p>
     * Note that just because an {@link FluidKey} passes this validity test, and is mergable with the current fluid,
     * does not mean that you can insert the fluid into this inventory. */
    @Override
    boolean isFluidValidForTank(int tank, FluidKey fluid);

    /** Sets the fluid in the given tank to the given fluid.
     * 
     * @return True if the modification was allowed, false otherwise. (For example if the given stack doesn't pass the
     *         {@link FixedFluidInvView#isFluidValidForTank(int, FluidKey)} test). */
    boolean setInvFluid(int tank, FluidVolume to, Simulation simulation);

    /** Sets the stack in the given slot to the given stack, or throws an exception if it was not permitted. */
    default void forceSetInvFluid(int slot, FluidVolume to) {
        if (!setInvFluid(slot, to, Simulation.ACTION)) {
            throw new IllegalStateException("Unable to force-set the tank " + slot + " to " + to + "!");
        }
    }

    /** Applies the given function to the stack held in the slot, and uses {@link #forceSetInvFluid(int, FluidVolume)}
     * on the result (Which will throw an exception if the returned stack is not valid for this inventory). */
    default void modifyTank(int tank, Function<FluidVolume, FluidVolume> function) {
        forceSetInvFluid(tank, function.apply(getInvFluid(tank)));
    }

    /** Attempts to insert the given fluid volume into the given tank, returning the excess.
     * <p>
     * (This is a tank-based version of {@link FluidInsertable#attemptInsertion(FluidVolume, Simulation)} - if you want
     * to use any of the other tank specific methods then it's recommended you get an {@link FluidInsertable} from
     * {@link #getTank(int)}).
     * 
     * @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @param volume The incoming fluid volume. Must not be modified by this call.
     * @param simulation If {@link Simulation#SIMULATE} then this shouldn't modify anything.
     * @return the excess {@link ItemStack} that wasn't accepted. This will be independent of this insertable, however
     *         it might be the given stack instead of a completely new object.
     * @throws RuntimeException if the given slot wasn't a valid index. */
    default FluidVolume insertFluid(int tank, FluidVolume volume, Simulation simulation) {
        FluidTransferResult result = FluidVolumeUtil.computeInsertion(getInvFluid(tank), getMaxAmount_F(tank), volume);
        if (result.result == volume) {
            return volume;
        }

        if (setInvFluid(tank, result.inTank, simulation)) {
            return result.result;
        } else {
            return volume;
        }
    }

    /** Attempts to extract part of the fluid volume that is held in the given tank.
     * <p>
     * This is a tank based version of {@link FluidExtractable#attemptExtraction(FluidFilter, FluidAmount, Simulation)},
     * however it includes a number of additional arguments. If you want to use any of the simpler methods than it's
     * recommenced that you get an {@link ItemExtractable} from {@link #getTank(int)}.
     * 
     * @param tank The slot index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @param filter If non-null then this will be checked against the stored stack to see if anything can be extracted.
     * @param mergeWith If non-empty then this will be merged with the extracted stack, and as such they should be
     *            equal.
     * @param maxAmount The maximum amount of fluid to extract.
     * @param simulation If {@link Simulation#SIMULATE} then this shouldn't modify anything.
     * @return mergeWith (if non-empty) or the extracted stack if it is empty. */
    default FluidVolume extractFluid(
        int tank, @Nullable FluidFilter filter, FluidVolume mergeWith, FluidAmount maxAmount, Simulation simulation
    ) {
        if (mergeWith == null) {
            mergeWith = FluidVolumeUtil.EMPTY;
        }
        FluidVolume inTank = getInvFluid(tank);
        FluidTransferResult result = FluidVolumeUtil.computeExtraction(inTank, filter, mergeWith, maxAmount);
        if (setInvFluid(tank, result.inTank, simulation)) {
            return result.result;
        } else {
            return mergeWith;
        }
    }

    @Override
    default SingleFluidTank getTank(int tank) {
        return new SingleFluidTank(this, tank);
    }

    @Override
    default Iterable<? extends SingleFluidTank> tankIterable() {
        return () -> new Iterator<SingleFluidTank>() {
            int index = 0;

            @Override
            public SingleFluidTank next() {
                return getTank(index++);
            }

            @Override
            public boolean hasNext() {
                return index < getTankCount();
            }
        };
    }

    /** @return A new {@link LimitedFixedFluidInv} that provides a more controllable version of this
     *         {@link FixedFluidInv}. */
    default LimitedFixedFluidInv createLimitedFixedInv() {
        return new SimpleLimitedFixedFluidInv(this);
    }

    /** @return An {@link FluidInsertable} for this inventory that will attempt to insert into any of the tanks in this
     *         inventory. */
    default FluidInsertable getInsertable() {
        return getGroupedInv();
    }

    /** @return An {@link FluidExtractable} for this inventory that will attempt to extract from any of the tanks in
     *         this inventory. */
    default FluidExtractable getExtractable() {
        return getGroupedInv();
    }

    /** @return An {@link FluidTransferable} for this inventory that will attempt to extract from any of the tanks in
     *         this inventory. */
    default FluidTransferable getTransferable() {
        return getGroupedInv();
    }

    @Override
    default GroupedFluidInv getGroupedInv() {
        return new GroupedFluidInvFixedWrapper(this);
    }

    @Override
    default FixedFluidInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        if (fromIndex == 0 && toIndex == getTankCount()) {
            return this;
        }
        return new SubFixedFluidInv(this, fromIndex, toIndex);
    }

    @Override
    default FixedFluidInv getMappedInv(int... tanks) {
        if (tanks.length == 0) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        if (tanks.length == getTankCount()) {
            boolean isThis = true;
            for (int i = 0; i < tanks.length; i++) {
                if (tanks[i] != i) {
                    isThis = false;
                    break;
                }
            }
            if (isThis) {
                return this;
            }
        }
        return new MappedFixedFluidInv(this, tanks);
    }
}
