package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.FixedItemInv;

/** A tracker object that tries to ensure that the "no modification" rule that methods like
 * {@link FixedItemInv#getInvStack(int)} use is followed. */
public final class ItemInvModificationTracker {
    private ItemInvModificationTracker() {}

    /** Tracks the given ItemStack to ensure that it hasn't changed by the time it is garbage collected. */
    public static void trackNeverChanging(ItemStack stack) {

        // TODO: Implement tracking!
    }
}
