package alexiil.mc.lib.attributes.item.impl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.AttributeUtil;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.impl.ItemInvModificationTracker.TrackedItemStackState;

/** A tracker object that tries to ensure that the "no modification" rule that methods like
 * {@link FixedItemInv#getInvStack(int)} use is followed. */
public final class ItemInvModificationTracker {
    private ItemInvModificationTracker() {}

    private static final boolean ENABLED = AttributeUtil.EXPENSIVE_DEBUG_CHECKS;
    private static final Map<ItemStack, TrackedItemStackState> stacks =
        Collections.synchronizedMap(new WeakHashMap<>());

    /** Tracks the given ItemStack to ensure that it hasn't changed by the time it is garbage collected. */
    public static void trackNeverChanging(ItemStack stack) {
        if (!ENABLED || stack == null) {
            return;
        }
        TrackedItemStackState ref = stacks.get(stack);
        if (ref != null) {
            ref.check(stack);
        } else {
            stacks.put(stack, new TrackedItemStackState(stack, new Throwable().fillInStackTrace()));
        }
    }

    final static class TrackedItemStackState {
        final ItemStack copy;
        final Throwable stack;

        public TrackedItemStackState(ItemStack referent, Throwable stack) {
            this.copy = referent.copy();
            this.stack = stack;
        }

        void check(ItemStack current) {
            if (ItemStack.areEqual(current, copy)) {
                return;
            }
            throw new IllegalStateException("The ItemStack that is stored has been changed! (\n\tOriginal = "
                + stackToFullString(copy) + ", \n\tChanged = " + stackToFullString(current) + ")", stack);
        }
    }

    public static String stackToFullString(ItemStack stack) {
        if (stack.isEmpty()) {
            return "Empty";
        }
        int amount = stack.getAmount();
        String stackStr = amount + "x" + stack.getTranslationKey();
        return stackStr + " tag = " + stack.getTag();
    }
}
