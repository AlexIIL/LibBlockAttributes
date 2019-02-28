package alexiil.mc.lib.attributes.item;

import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.CombinedFixedItemInvView;
import alexiil.mc.lib.attributes.item.impl.CombinedItemExtractable;
import alexiil.mc.lib.attributes.item.impl.CombinedItemInsertable;
import alexiil.mc.lib.attributes.item.impl.CombinedItemInvStats;
import alexiil.mc.lib.attributes.item.impl.EmptyFixedItemInv;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.EmptyItemInvStats;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;

public class ItemAttributes {

    public static final CombinableAttribute<IFixedItemInvView> FIXED_INV_VIEW;
    public static final CombinableAttribute<IFixedItemInv> FIXED_INV;
    public static final CombinableAttribute<IItemInvStats> INV_STATS;
    public static final CombinableAttribute<IItemInsertable> INSERTABLE;
    public static final CombinableAttribute<IItemExtractable> EXTRACTABLE;

    static {
        FIXED_INV_VIEW = c(IFixedItemInvView.class, EmptyFixedItemInv.INSTANCE, CombinedFixedItemInvView::new);
        FIXED_INV = c(IFixedItemInv.class, EmptyFixedItemInv.INSTANCE, CombinedFixedItemInv::new);

        // For some reason the java compiler can't work out what <T> should be for these three
        // So instead we create a lambda, which somehow gives it enough space to work out what it is.
        // (and yet eclipse had no problems with it :/ )
        INV_STATS = c(IItemInvStats.class, EmptyItemInvStats.INSTANCE, list -> new CombinedItemInvStats(list));
        INSERTABLE = c(IItemInsertable.class, RejectingItemInsertable.NULL, list -> new CombinedItemInsertable(list));
        EXTRACTABLE = c(IItemExtractable.class, EmptyItemExtractable.NULL, list -> new CombinedItemExtractable(list));
    }

    private static <T> CombinableAttribute<T> c(Class<T> clazz, T nullInstance,
        CombinableAttribute.IAttributeCombiner<T> combiner) {
        return new CombinableAttribute<>(clazz, nullInstance, combiner);
    }
}
