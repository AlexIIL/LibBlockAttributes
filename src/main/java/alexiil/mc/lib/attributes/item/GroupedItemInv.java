package alexiil.mc.lib.attributes.item;

import alexiil.mc.lib.attributes.item.impl.SimpleLimitedGroupedItemInv;

/** A modifiable version of {@link GroupedItemInvView}, except that all modification methods are provided by
 * {@link ItemExtractable} and {@link ItemInsertable}. */
public interface GroupedItemInv extends GroupedItemInvView, ItemTransferable {

    /** @return A new {@link LimitedGroupedItemInv} that provides a more controllable version of this
     *         {@link GroupedItemInv}. */
    default LimitedGroupedItemInv createLimitedInv() {
        return new SimpleLimitedGroupedItemInv(this);
    }
}
