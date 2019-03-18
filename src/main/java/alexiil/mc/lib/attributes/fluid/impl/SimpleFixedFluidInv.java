package alexiil.mc.lib.attributes.fluid.impl;

import java.lang.reflect.Method;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.SystemUtil;

import alexiil.mc.lib.attributes.AttributeUtil;
import alexiil.mc.lib.attributes.IListenerRemovalToken;
import alexiil.mc.lib.attributes.IListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.IFluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

/** A simple, extendible, fixed size item inventory that supports all of the features that {@link IFixedFluidInv}
 * exposes.
 * <p>
 * Extending classes should take care to override {@link #getFilterForTank(int)} if they also override
 * {@link #isFluidValidForTank(int, FluidKey)}.
 * <p>
 * Note: Generally it is better to extend {@link JumboFixedFluidInv} for inventories with a large number of similar
 * tanks (like a chest). */
public class SimpleFixedFluidInv implements IFixedFluidInv {

    private static final IFluidInvTankChangeListener[] NO_LISTENERS = new IFluidInvTankChangeListener[0];

    /** Sentinel value used during {@link #invalidateListeners()}. */
    private static final IFluidInvTankChangeListener[] INVALIDATING_LISTENERS = new IFluidInvTankChangeListener[0];

    public final int tankCapacity;
    protected final DefaultedList<FluidVolume> tanks;

    private final Map<IFluidInvTankChangeListener, IListenerRemovalToken> listeners =
        new Object2ObjectLinkedOpenCustomHashMap<>(SystemUtil.identityHashStrategy());

    // Should this use WeakReference instead of storing them directly?
    private IFluidInvTankChangeListener[] bakedListeners = NO_LISTENERS;

    public SimpleFixedFluidInv(int invSize, int tankCapacity) {
        tanks = DefaultedList.create(invSize, FluidKeys.EMPTY.withAmount(0));
        this.tankCapacity = tankCapacity;
    }

    // IFixedFluidInv

    @Override
    public final int getTankCount() {
        return tanks.size();
    }

    @Override
    public int getMaxAmount(int tank) {
        return tankCapacity;
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
    public IFluidFilter getFilterForTank(int tank) {
        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
            Class<?> cls = getClass();
            if (cls != SimpleFixedFluidInv.class) {
                try {
                    Method method = cls.getMethod("isFluidValidForTank", int.class, FluidKey.class);
                    if (method.getDeclaringClass() != SimpleFixedFluidInv.class) {
                        // it's been overriden, but we haven't
                        throw new IllegalStateException("The subclass " + method.getDeclaringClass()
                            + " has overriden isFluidValidForTank() but hasn't overriden getFilterForTank()");
                    }
                } catch (ReflectiveOperationException roe) {
                    throw new Error(
                        "Failed to get the isFluidValidForTank method! I'm not sure what to do now, as this shouldn't happen normally :(",
                        roe);
                }
            }
        }
        return ConstantFluidFilter.ANYTHING;
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        if (isFluidValidForTank(tank, to.fluidKey) && to.getAmount() <= getMaxAmount(tank)) {
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
    public IListenerToken addListener(IFluidInvTankChangeListener listener, IListenerRemovalToken removalToken) {
        if (bakedListeners == INVALIDATING_LISTENERS) {
            // It doesn't really make sense to add listeners while we are invalidating them
            return null;
        }
        IListenerRemovalToken previous = listeners.put(listener, removalToken);
        if (previous == null) {
            bakeListeners();
        } else {
            assert previous == removalToken : "The same listener object must be registered with the same removal token";
        }
        return () -> {
            IListenerRemovalToken token = listeners.remove(listener);
            if (token != null) {
                assert token == removalToken;
                bakeListeners();
                removalToken.onListenerRemoved();
            }
        };
    }

    private void bakeListeners() {
        bakedListeners = listeners.keySet().toArray(new IFluidInvTankChangeListener[0]);
    }

    public void invalidateListeners() {
        bakedListeners = INVALIDATING_LISTENERS;
        IListenerRemovalToken[] removalTokens = listeners.values().toArray(new IListenerRemovalToken[0]);
        listeners.clear();
        for (IListenerRemovalToken token : removalTokens) {
            token.onListenerRemoved();
        }
        bakedListeners = NO_LISTENERS;
    }

    protected final void fireTankChange(int tank, FluidVolume previous, FluidVolume current) {
        // Iterate over the previous array in case the listeners array is changed while we are iterating
        final IFluidInvTankChangeListener[] baked = bakedListeners;
        for (IFluidInvTankChangeListener listener : baked) {
            listener.onChange(this, tank, previous, current);
        }
    }

    // NBT support

    public final CompoundTag toTag() {
        return toTag(new CompoundTag());
    }

    public CompoundTag toTag(CompoundTag tag) {
        ListTag tanksTag = new ListTag();
        for (FluidVolume volume : tanks) {
            tanksTag.add(volume.toTag());
        }
        tag.put("tanks", tanksTag);
        return tag;
    }

    public void fromTag(CompoundTag tag) {
        ListTag tanksTag = tag.getList("tanks", new CompoundTag().getType());
        for (int i = 0; i < tanksTag.size() && i < tanks.size(); i++) {
            tanks.set(i, FluidVolume.fromTag(tanksTag.getCompoundTag(i)));
        }
        for (int i = tanksTag.size(); i < tanks.size(); i++) {
            tanks.set(i, FluidKeys.EMPTY.withAmount(0));
        }
    }
}
