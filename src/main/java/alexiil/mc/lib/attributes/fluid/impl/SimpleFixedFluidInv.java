package alexiil.mc.lib.attributes.fluid.impl;

import java.lang.reflect.Method;
import java.util.Set;

import net.minecraft.util.DefaultedList;
import net.minecraft.util.SystemUtil;

import alexiil.mc.lib.attributes.AttributeUtil;
import alexiil.mc.lib.attributes.IListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidKey;
import alexiil.mc.lib.attributes.fluid.FluidVolume;
import alexiil.mc.lib.attributes.fluid.IFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.IFluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;

/** A simple, extendible, fixed size item inventory that supports all of the features that {@link IFixedFluidInv}
 * exposes.
 * <p>
 * Extending classes should take care to override {@link #getFilterForTank(int)} if they also override
 * {@link #isFluidValidForTank(int, FluidKey)}.
 * <p>
 * Note: Generally it is better to extend {@link JumboFixedFluidInv} for inventories with a large number of similar
 * tanks (like a chest). */
public class SimpleFixedFluidInv implements IFixedFluidInv {

    // TODO: NBT serialisation and a test mod with chests!

    private static final IFluidInvTankChangeListener[] NO_LISTENERS = new IFluidInvTankChangeListener[0];

    public final int tankCapacity;
    protected final DefaultedList<FluidVolume> tanks;

    private final Set<IFluidInvTankChangeListener> listeners =
        new ObjectLinkedOpenCustomHashSet<>(SystemUtil.identityHashStrategy());

    // Should this use WeakReference instead of storing them directly?
    private IFluidInvTankChangeListener[] bakedListeners = NO_LISTENERS;

    public SimpleFixedFluidInv(int invSize, int tankCapacity) {
        tanks = DefaultedList.create(invSize, new FluidVolume());
        this.tankCapacity = tankCapacity;
    }

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
    public IListenerToken addListener(IFluidInvTankChangeListener listener) {
        if (listeners.add(listener)) {
            bakeListeners();
        }
        return () -> {
            if (listeners.remove(listener)) {
                bakeListeners();
            }
        };
    }

    private void bakeListeners() {
        bakedListeners = listeners.toArray(new IFluidInvTankChangeListener[0]);
    }

    protected final void fireTankChange(int tank, FluidVolume previous, FluidVolume current) {
        // Iterate over the previous array in case the listeners array is changed while we are iterating
        final IFluidInvTankChangeListener[] baked = bakedListeners;
        for (IFluidInvTankChangeListener listener : baked) {
            listener.onChange(this, tank, previous, current);
        }
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        if (isFluidValidForTank(tank, to.toKey()) && to.getAmount() <= getMaxAmount(tank)) {
            if (simulation == Simulation.ACTION) {
                FluidVolume before = tanks.get(tank);
                tanks.set(tank, to);
                fireTankChange(tank, before, to);
            }
            return true;
        }
        return false;
    }
}
