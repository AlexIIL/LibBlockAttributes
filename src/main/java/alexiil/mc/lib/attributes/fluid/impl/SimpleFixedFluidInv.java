/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.util.Map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;

import alexiil.mc.lib.attributes.AttributeUtil;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.FluidTransferable;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.Saveable;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

/** A simple, extendible, fixed size item inventory that supports all of the features that {@link FixedFluidInv}
 * exposes.
 * <p>
 * Extending classes should take care to override {@link #getFilterForTank(int)} if they also override
 * {@link #isFluidValidForTank(int, FluidKey)}.
 * <p>
 * Note: Generally it is better to extend {@link JumboFixedFluidInv} for inventories with a large number of similar
 * tanks (like a chest). */
public class SimpleFixedFluidInv implements FixedFluidInv, FluidTransferable, Saveable {

    private static final FluidInvTankChangeListener[] NO_LISTENERS = new FluidInvTankChangeListener[0];

    /** Sentinel value used during {@link #invalidateListeners()}. */
    private static final FluidInvTankChangeListener[] INVALIDATING_LISTENERS = new FluidInvTankChangeListener[0];

    /** @deprecated Replaced by {@link #tankCapacity_F} */
    @Deprecated(since = "0.6.0", forRemoval = true)
    public final int tankCapacity;

    public final FluidAmount tankCapacity_F;
    protected final DefaultedList<FluidVolume> tanks;

    // TODO: Optimise this to cache more information!
    private final GroupedFluidInv groupedVersion = new GroupedFluidInvFixedWrapper(this);

    private FluidInvTankChangeListener ownerListener;

    private final Map<FluidInvTankChangeListener, ListenerRemovalToken> listeners
        = new Object2ObjectLinkedOpenCustomHashMap<>(Util.identityHashStrategy());

    // Should this use WeakReference instead of storing them directly?
    private FluidInvTankChangeListener[] bakedListeners = NO_LISTENERS;

    /** @deprecated Replaced by {@link #SimpleFixedFluidInv(int, FluidAmount)}. */
    @Deprecated(since = "0.6.0", forRemoval = true)
    public SimpleFixedFluidInv(int invSize, int tankCapacity) {
        this(invSize, FluidAmount.of1620(tankCapacity));
    }

    public SimpleFixedFluidInv(int invSize, FluidAmount tankCapacity) {
        tanks = DefaultedList.ofSize(invSize, FluidVolumeUtil.EMPTY);
        this.tankCapacity = tankCapacity.as1620(RoundingMode.DOWN);
        this.tankCapacity_F = tankCapacity;
    }

    // IFixedFluidInv

    @Override
    public final int getTankCount() {
        return tanks.size();
    }

    /** @deprecated Replaced by {@link #getMaxAmount_F(int)}. */
    @Override
    @Deprecated(since = "0.6.0", forRemoval = true)
    public int getMaxAmount(int tank) {
        return tankCapacity;
    }

    @Override
    public FluidAmount getMaxAmount_F(int tank) {
        return tankCapacity_F;
    }

    @Override
    public FluidVolume getInvFluid(int tank) {
        return tanks.get(tank);
    }

    @Override
    public boolean isFluidValidForTank(int tank, FluidKey fluid) {
        return true;
    }

    @Override
    public FluidFilter getFilterForTank(int tank) {
        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
            Class<?> cls = getClass();
            if (cls != SimpleFixedFluidInv.class) {
                try {
                    Method method = cls.getMethod("isFluidValidForTank", int.class, FluidKey.class);
                    if (method.getDeclaringClass() != SimpleFixedFluidInv.class) {
                        // it's been overriden, but we haven't
                        throw new IllegalStateException(
                            "The subclass " + method.getDeclaringClass()
                                + " has overriden isFluidValidForTank() but hasn't overriden getFilterForTank()"
                        );
                    }
                } catch (ReflectiveOperationException roe) {
                    throw new Error(
                        "Failed to get the isFluidValidForTank method! I'm not sure what to do now, as this shouldn't happen normally :(",
                        roe
                    );
                }
            }
        }
        return ConstantFluidFilter.ANYTHING;
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        if (isFluidValidForTank(tank, to.fluidKey) && !to.amount().isGreaterThan(getMaxAmount_F(tank))) {
            if (simulation == Simulation.ACTION) {
                FluidVolume before = tanks.get(tank);
                tanks.set(tank, to);
                fireTankChange(tank, before, to);
            }
            return true;
        }
        return false;
    }

    // Others

    @Override
    public GroupedFluidInv getGroupedInv() {
        return this.groupedVersion;
    }

    @Override
    public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
        if (bakedListeners == INVALIDATING_LISTENERS) {
            // It doesn't really make sense to add listeners while we are invalidating them
            return null;
        }
        ListenerRemovalToken previous = listeners.put(listener, removalToken);
        if (previous == null) {
            bakeListeners();
        } else {
            assert previous == removalToken : "The same listener object must be registered with the same removal token";
        }
        return () -> {
            ListenerRemovalToken token = listeners.remove(listener);
            if (token != null) {
                assert token == removalToken;
                bakeListeners();
                removalToken.onListenerRemoved();
            }
        };
    }

    /** Sets the owner listener callback, which is never removed from the listener list when
     * {@link #invalidateListeners()} is called. */
    public void setOwnerListener(FluidInvTankChangeListener ownerListener) {
        this.ownerListener = ownerListener;
    }

    private void bakeListeners() {
        bakedListeners = listeners.keySet().toArray(new FluidInvTankChangeListener[0]);
    }

    public void invalidateListeners() {
        bakedListeners = INVALIDATING_LISTENERS;
        ListenerRemovalToken[] removalTokens = listeners.values().toArray(new ListenerRemovalToken[0]);
        listeners.clear();
        for (ListenerRemovalToken token : removalTokens) {
            token.onListenerRemoved();
        }
        bakedListeners = NO_LISTENERS;
    }

    protected final void fireTankChange(int tank, FluidVolume previous, FluidVolume current) {
        if (ownerListener != null) {
            ownerListener.onChange(this, tank, previous, current);
        }
        // Iterate over the previous array in case the listeners array is changed while we are iterating
        final FluidInvTankChangeListener[] baked = bakedListeners;
        for (FluidInvTankChangeListener listener : baked) {
            listener.onChange(this, tank, previous, current);
        }
    }

    // NBT support

    @Override
    public final NbtCompound toTag() {
        return toTag(new NbtCompound());
    }

    @Override
    public NbtCompound toTag(NbtCompound tag) {
        NbtList tanksTag = new NbtList();
        for (FluidVolume volume : tanks) {
            tanksTag.add(volume.toTag());
        }
        tag.put("tanks", tanksTag);
        return tag;
    }

    @Override
    public void fromTag(NbtCompound tag) {
        NbtList tanksTag = tag.getList("tanks", new NbtCompound().getType());
        for (int i = 0; i < tanksTag.size() && i < tanks.size(); i++) {
            tanks.set(i, FluidVolume.fromTag(tanksTag.getCompound(i)));
        }
        for (int i = tanksTag.size(); i < tanks.size(); i++) {
            tanks.set(i, FluidVolumeUtil.EMPTY);
        }
    }

    // FluidInsertable

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        return groupedVersion.attemptInsertion(fluid, simulation);
    }

    @Override
    public FluidAmount getMinimumAcceptedAmount() {
        return groupedVersion.getMinimumAcceptedAmount();
    }

    @Override
    public FluidFilter getInsertionFilter() {
        return groupedVersion.getInsertionFilter();
    }

    // FluidExtractable

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        return groupedVersion.attemptExtraction(filter, maxAmount, simulation);
    }

    @Override
    public FluidVolume attemptAnyExtraction(FluidAmount maxAmount, Simulation simulation) {
        return groupedVersion.attemptAnyExtraction(maxAmount, simulation);
    }
}
