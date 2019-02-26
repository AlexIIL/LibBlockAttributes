package alexiil.mc.lib.attributes.item.impl;

import java.lang.reflect.Method;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.SystemUtil;

import alexiil.mc.lib.attributes.AttributeDebugging;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IFixedItemInv;
import alexiil.mc.lib.attributes.item.IInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;

/** A simple, extendible, fixed size item inventory that supports all of the features that {@link IFixedItemInv}
 * exposes.
 * <p>
 * Extending classes should take care to override {@link #getFilterForSlot(int)} if they also override
 * {@link #isItemValidForSlot(int, ItemStack)}.
 * <p>
 * Note: Generally it is better to extend {@link JumboFixedItemInv} for inventories with a large number of similar slots
 * (like a chest). */
public class SimpleFixedItemInv implements IFixedItemInv {

    // TODO: NBT serialisation and a test mod with chests!

    private static final IInvSlotChangeListener[] NO_LISTENERS = new IInvSlotChangeListener[0];

    protected final DefaultedList<ItemStack> slots;

    private final Set<IInvSlotChangeListener> listeners =
        new ObjectLinkedOpenCustomHashSet<>(SystemUtil.identityHashStrategy());

    // Should this use WeakReference instead of storing them directly?
    private IInvSlotChangeListener[] bakedListeners = NO_LISTENERS;

    public SimpleFixedItemInv(int invSize) {
        slots = DefaultedList.create(invSize, ItemStack.EMPTY);
    }

    @Override
    public final int getInvSize() {
        return slots.size();
    }

    @Override
    public ItemStack getInvStack(int slot) {
        return slots.get(slot);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return true;
    }

    @Override
    public IItemFilter getFilterForSlot(int slot) {
        if (AttributeDebugging.DEBUG_CHECK_EVERYTHING) {
            Class<?> cls = getClass();
            if (cls != SimpleFixedItemInv.class) {
                try {
                    Method method = cls.getMethod("isItemValidForSlot", int.class, ItemStack.class);
                    if (method.getDeclaringClass() != SimpleFixedItemInv.class) {
                        // it's been overriden, but we haven't
                        throw new IllegalStateException("The subclass " + method.getDeclaringClass()
                            + " has overriden isItemValidForSlot() but hasn't overriden getFilterForSlot()");
                    }
                } catch (ReflectiveOperationException roe) {
                    throw new Error(
                        "Failed to get the isItemValidForSlot method! I'm not sure what to do now, as this shouldn't happen normally :(",
                        roe);
                }
            }
        }
        return IItemFilter.ANY_STACK;
    }

    @Override
    public IListenerToken addListener(IInvSlotChangeListener listener) {
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
        bakedListeners = listeners.toArray(new IInvSlotChangeListener[0]);
    }

    protected final void fireSlotChange(int slot, ItemStack previous, ItemStack current) {
        // Iterate over the previous array in case the listeners array is changed while we are iterating
        final IInvSlotChangeListener[] baked = bakedListeners;
        for (IInvSlotChangeListener listener : baked) {
            listener.onChange(this, slot, previous, current);
        }
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        if (isItemValidForSlot(slot, to) && to.getAmount() <= getMaxAmount(slot, to)) {
            if (simulation == Simulation.ACTION) {
                ItemStack before = slots.get(slot);
                slots.set(slot, to);
                fireSlotChange(slot, before, to);
            }
            return true;
        }
        return false;
    }
}
