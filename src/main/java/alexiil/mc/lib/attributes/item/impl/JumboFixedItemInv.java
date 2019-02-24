package alexiil.mc.lib.attributes.item.impl;

import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.IItemInsertable;
import alexiil.mc.lib.attributes.item.IItemInvStats;

/** A {@link SimpleFixedItemInv} with a few optimisations to make the {@link IItemInsertable}, {@link IItemExtractable},
 * and {@link IItemInvStats} implementations much faster than a simple inventory search for larger inventories. */
public class JumboFixedItemInv extends SimpleFixedItemInv {

    public JumboFixedItemInv(int invSize) {
        super(invSize);
    }

    // TODO: Optimisations!
}
