package alexiil.mc.lib.attributes.item;

/** Listener {@link FunctionalInterface} for
 * {@link FixedItemInvView#addListener(InvMarkDirtyListener, alexiil.mc.lib.attributes.ListenerRemovalToken)}. */
@FunctionalInterface
public interface InvMarkDirtyListener {

    /** @param inv The inventory that was modified - this is always the inventory object that you registered the
     *            listener with, and never any delegate inventories! */
    void onMarkDirty(AbstractItemInvView inv);
}
