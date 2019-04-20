package alexiil.mc.lib.attributes.item.impl;

import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.GroupedItemInvView;

/** A {@link SimpleFixedItemInv} with a few optimisations to make the {@link ItemInsertable}, {@link ItemExtractable},
 * and {@link GroupedItemInvView} implementations much faster than a simple inventory search for larger inventories. */
public class JumboFixedItemInv extends SimpleFixedItemInv {

    public JumboFixedItemInv(int invSize) {
        super(invSize);
    }

    // TODO: Optimisations!
}
