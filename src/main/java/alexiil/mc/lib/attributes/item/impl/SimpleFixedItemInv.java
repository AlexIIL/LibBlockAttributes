package alexiil.mc.lib.attributes.item.impl;

import java.lang.reflect.Method;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.SystemUtil;

import alexiil.mc.lib.attributes.AttributeUtil;
import alexiil.mc.lib.attributes.IListenerRemovalToken;
import alexiil.mc.lib.attributes.IListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IFixedItemInv;
import alexiil.mc.lib.attributes.item.IItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

/** A simple, extendible, fixed size item inventory that supports all of the features that {@link IFixedItemInv}
 * exposes.
 * <p>
 * Extending classes should take care to override {@link #getFilterForSlot(int)} if they also override
 * {@link #isItemValidForSlot(int, ItemStack)}.
 * <p>
 * Note: Generally it is better to extend/use {@link JumboFixedItemInv} for inventories with a large number of similar
 * slots (like a chest). */
public class SimpleFixedItemInv implements IFixedItemInv {

    private static final IItemInvSlotChangeListener[] NO_LISTENERS = new IItemInvSlotChangeListener[0];

    /** Sentinel value used during {@link #invalidateListeners()}. */
    private static final IItemInvSlotChangeListener[] INVALIDATING_LISTENERS = new IItemInvSlotChangeListener[0];

    protected final DefaultedList<ItemStack> slots;

    private final Map<IItemInvSlotChangeListener, IListenerRemovalToken> listeners =
        new Object2ObjectLinkedOpenCustomHashMap<>(SystemUtil.identityHashStrategy());

    // Should this use WeakReference instead of storing them directly?
    private IItemInvSlotChangeListener[] bakedListeners = NO_LISTENERS;

    public SimpleFixedItemInv(int invSize) {
        slots = DefaultedList.create(invSize, ItemStack.EMPTY);
    }

    @Override
    public final int getSlotCount() {
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
        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
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
        return ConstantItemFilter.ANYTHING;
    }

    @Override
    public IListenerToken addListener(IItemInvSlotChangeListener listener, IListenerRemovalToken removalToken) {
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
        bakedListeners = listeners.keySet().toArray(new IItemInvSlotChangeListener[0]);
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

    protected final void fireSlotChange(int slot, ItemStack previous, ItemStack current) {
        // Iterate over the previous array in case the listeners array is changed while we are iterating
        final IItemInvSlotChangeListener[] baked = bakedListeners;
        for (IItemInvSlotChangeListener listener : baked) {
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

    // NBT support

    public final CompoundTag toTag() {
        return toTag(new CompoundTag());
    }

    public CompoundTag toTag(CompoundTag tag) {
        ListTag tanksTag = new ListTag();
        for (ItemStack stack : slots) {
            tanksTag.add(stack.toTag(new CompoundTag()));
        }
        tag.put("slots", tanksTag);
        return tag;
    }

    public void fromTag(CompoundTag tag) {
        ListTag slotsTag = tag.getList("slots", new CompoundTag().getType());
        for (int i = 0; i < slotsTag.size() && i < slots.size(); i++) {
            slots.set(i, ItemStack.fromTag(slotsTag.getCompoundTag(i)));
        }
        for (int i = slotsTag.size(); i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
    }
}
