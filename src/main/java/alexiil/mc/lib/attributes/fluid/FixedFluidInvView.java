/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.shape.VoxelShape;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.AttributeUtil;
import alexiil.mc.lib.attributes.CacheInfo;
import alexiil.mc.lib.attributes.Convertible;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.GroupedFluidInvViewFixedWrapper;
import alexiil.mc.lib.attributes.fluid.impl.MappedFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.SubFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.SubFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A view of a fixed inventory for fluids, where the number of tanks never changes, and every tank is "simple":
 * <ul>
 * <li>The fluid will stay in the tank until it is removed or changed by something else. (So setting the fluid in a tank
 * of an {@link FixedFluidInv} will reflect that change in {@link #getInvFluid(int)}).</li>
 * </ul>
 * <p>
 * The attribute is stored in {@link FluidAttributes#FIXED_INV_VIEW}.
 * <p>
 * There are various classes of interest:
 * <ul>
 * <li>A modifiable version of this is {@link FixedFluidInv}.</li>
 * <li>The null instance is {@link EmptyFixedFluidInv}</li>
 * <li>A combined view of several sub-inventories is {@link CombinedFixedFluidInvView}.</li>
 * <li>A partial view of a single inventory is {@link SubFixedFluidInv}</li>
 * </ul>
 */
public interface FixedFluidInvView extends Convertible {

    /** @return The number of tanks in this inventory. */
    int getTankCount();

    /** @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return The FluidVolume that is held in the tank at the moment. The returned volume must never be modified!
     * @throws RuntimeException if the given tank wasn't a valid index. */
    FluidVolume getInvFluid(int tank);

    /** @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return The maximum amount that the given tank can hold of the given fluid. This method will ignore the current
     *         stack in {@link #getInvFluid(int)}. Note that any setters that this object implements (like
     *         {@link FixedFluidInv#setInvFluid(int, FluidVolume, Simulation)} should reject stacks that are greater
     *         than this value. (and callers should only call this if they need to check the amounts separately. Note
     *         that it is meaningless to return values greater than the maximum amount an fluid can be stacked to here,
     *         and callers are free to throw an exception if this is violated. (Basically huge single-tank inventories
     *         shouldn't implement this interface).
     * @throws RuntimeException if the given tank wasn't a valid index.
     * @deprecated Replaced by {@link #getMaxAmount_F(int)} */
    @Deprecated(since = "0.6.0", forRemoval = true)
    default int getMaxAmount(int tank) {
        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
            validateFixedFluidInvView(this);
        }
        return getMaxAmount_F(tank).as1620();
    }

    /** @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return The maximum amount that the given tank can hold of the given fluid. This method will ignore the current
     *         stack in {@link #getInvFluid(int)}. Note that any setters that this object implements (like
     *         {@link FixedFluidInv#setInvFluid(int, FluidVolume, Simulation)} should reject stacks that are greater
     *         than this value. (and callers should only call this if they need to check the amounts separately. Note
     *         that it is meaningless to return values greater than the maximum amount an fluid can be stacked to here,
     *         and callers are free to throw an exception if this is violated. (Basically huge single-tank inventories
     *         shouldn't implement this interface).
     * @throws RuntimeException if the given tank wasn't a valid index. */
    default FluidAmount getMaxAmount_F(int tank) {
        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
            validateFixedFluidInvView(this);
        }
        return FluidAmount.of1620(getMaxAmount(tank));
    }

    /** Checks to see if the given fluid would be valid for this tank, ignoring current contents. Note that this method
     * should adhere to the requirements of {@link FluidFilter#matches(FluidKey)}, so passing {@link FluidKey#isEmpty()
     * empty} fluids will generally not return useful results.
     * 
     * @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @throws RuntimeException if the given tank wasn't a valid index. */
    boolean isFluidValidForTank(int tank, FluidKey fluid);

    /** Exposes {@link #isFluidValidForTank(int, FluidKey)} as a (potentially) readable filter.
     * 
     * @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return An {@link FluidFilter} for this tank. If this tank is filtered by an {@link FluidFilter} internally then
     *         it is highly recommended that this be overridden to return <em>that</em> filter rather than the default
     *         opaque wrapper around {@link #isFluidValidForTank(int, FluidKey)}.
     * @throws RuntimeException if the given tank wasn't a valid index. */
    default FluidFilter getFilterForTank(int tank) {
        return stack -> isFluidValidForTank(tank, stack);
    }

    default SingleFluidTankView getTank(int tank) {
        return new SingleFluidTankView(this, tank);
    }

    default Iterable<? extends SingleFluidTankView> tankIterable() {
        return () -> new Iterator<SingleFluidTankView>() {
            int index = 0;

            @Override
            public SingleFluidTankView next() {
                return getTank(index++);
            }

            @Override
            public boolean hasNext() {
                return index < getTankCount();
            }
        };
    }

    default Iterable<FluidVolume> fluidIterable() {
        return () -> new Iterator<FluidVolume>() {
            int index = 0;

            @Override
            public FluidVolume next() {
                return getInvFluid(index);
            }

            @Override
            public boolean hasNext() {
                return index < getTankCount();
            }
        };
    }

    /** @return A statistical view of this inventory. */
    default GroupedFluidInvView getGroupedInv() {
        return new GroupedFluidInvViewFixedWrapper(this);
    }

    /** Adds the given listener to this inventory, such that the
     * {@link FluidInvTankChangeListener#onChange(FixedFluidInvView, int, FluidVolume, FluidVolume)} will be called
     * every time that this inventory changes. However if this inventory doesn't support listeners then this will return
     * a null {@link ListenerToken token}.
     * 
     * @param removalToken A token that will be called whenever the given listener is removed from this inventory (or if
     *            this inventory itself is unloaded or otherwise invalidated).
     * @return A token that represents the listener, or null if the listener could not be added. */
    default ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
        return null;
    }

    /** Equivalent to {@link List#subList(int, int)}.
     * 
     * @param fromIndex The first tank to expose
     * @param toIndex The tank after the last tank to expose.
     * @return a view of this inventory that only exposes the given number of tanks. Might return "this" if fromIndex is
     *         0 and toIndex is equal to {@link #getTankCount()}.
     * @throws RuntimeException if any of the given tanks weren't valid. */
    default FixedFluidInvView getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        if (fromIndex == 0 && toIndex == getTankCount()) {
            return this;
        }
        return new SubFixedFluidInvView(this, fromIndex, toIndex);
    }

    /** @param tanks The tanks to expose.
     * @return a view of this inventory that only exposes the given number of tanks. Might return "this" if the tank
     *         array is just [0,1, ... {@link #getTankCount()}-1]
     * @throws RuntimeException if any of the given tanks weren't valid */
    default FixedFluidInvView getMappedInv(int... tanks) {
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
        return new MappedFixedFluidInvView(this, tanks);
    }

    /** Offers this object and {@link #getGroupedInv()} to the attribute list. (Which, in turn, adds
     * {@link FixedFluidInv#getInsertable()}, {@link FixedFluidInv#getExtractable()}, and
     * {@link FixedFluidInv#getTransferable()} to the list as well).
     * 
     * @deprecated Because this functionality has been fully replaced by {@link Convertible} and it's usage in
     *             {@link AttributeList}. */
    @Deprecated(since = "0.4.9", forRemoval = true)
    default void offerSelfAsAttribute(
        AttributeList<?> list, @Nullable CacheInfo cacheInfo, @Nullable VoxelShape shape
    ) {
        list.offer(this, cacheInfo, shape);
    }

    @Override
    default <T> T convertTo(Class<T> otherType) {
        return Convertible.offer(otherType, getGroupedInv());
    }

    public static void validateFixedFluidInvView(FixedFluidInvView instance) {
        Class<?> c = instance.getClass();
        try {
            Method m0 = c.getDeclaredMethod("getMaxAmount", int.class);
            Method m1 = c.getDeclaredMethod("getMaxAmount_F", int.class);
            if (m0.getDeclaringClass() == FixedFluidInvView.class) {
                if (m1.getDeclaringClass() == FixedFluidInvView.class) {
                    throw new Error("The " + c + " needs to override either getMaxAmount() or getMaxAmount_F()!");
                }
            }
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }

    /** @return An object that only implements {@link FixedFluidInvView}, and does not expose the modification methods
     *         that {@link FixedFluidInv} does. Implementations that don't expose any modification methods themselves
     *         should override this method to just return themselves. */
    default FixedFluidInvView getFixedView() {
        final FixedFluidInvView real = this;
        return new FixedFluidInvView() {
            @Override
            public int getTankCount() {
                return real.getTankCount();
            }

            @Override
            public FluidVolume getInvFluid(int tank) {
                return real.getInvFluid(tank);
            }

            @Override
            public boolean isFluidValidForTank(int tank, FluidKey fluid) {
                return real.isFluidValidForTank(tank, fluid);
            }

            @Override
            @Deprecated // Will remove at the same time as super.
            public int getMaxAmount(int tank) {
                return real.getMaxAmount(tank);
            }

            @Override
            public FluidAmount getMaxAmount_F(int tank) {
                return real.getMaxAmount_F(tank);
            }

            @Override
            public FluidFilter getFilterForTank(int tank) {
                return real.getFilterForTank(tank);
            }

            @Override
            public GroupedFluidInvView getGroupedInv() {
                return new GroupedFluidInvViewFixedWrapper(this);
            }

            @Override
            public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
                final FixedFluidInvView view = this;
                return real.addListener(
                    (inv, tank, prev, curr) -> {
                        // Defend against giving the listener the real (possibly changeable) inventory.
                        // In addition the listener would probably cache *this view* rather than the backing inventory
                        // so they most likely need it to be this inventory.
                        listener.onChange(view, tank, prev, curr);
                    }, removalToken
                );
            }
        };
    }
}
