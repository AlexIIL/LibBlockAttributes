package alexiil.mc.lib.attributes.item;

/** A modifiable version of {@link GroupedItemInvView}, except that all modification methods are provided by
 * {@link ItemExtractable} and {@link ItemInsertable}. */
public interface GroupedItemInv extends GroupedItemInvView, ItemTransferable {
    // Nothing to declare here as all of the relevant modification methods are already declared in ItemTransferable
}
