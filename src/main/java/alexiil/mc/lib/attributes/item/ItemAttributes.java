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
        FIXED_INV_VIEW = create(IFixedItemInvView.class, EmptyFixedItemInv.INSTANCE, CombinedFixedItemInvView::new);
        FIXED_INV = create(IFixedItemInv.class, EmptyFixedItemInv.INSTANCE, CombinedFixedItemInv::new);
        INV_STATS = create(IItemInvStats.class, EmptyItemInvStats.INSTANCE, CombinedItemInvStats::new);
        INSERTABLE = create(IItemInsertable.class, RejectingItemInsertable.NULL, CombinedItemInsertable::new);
        EXTRACTABLE = create(IItemExtractable.class, EmptyItemExtractable.NULL, CombinedItemExtractable::new);
    }

    private static <T> CombinableAttribute<T> create(Class<T> clazz, T nullInstance,
        CombinableAttribute.IAttributeCombiner<T> combiner) {
        return new CombinableAttribute<>(clazz, nullInstance, combiner);
    }
}
