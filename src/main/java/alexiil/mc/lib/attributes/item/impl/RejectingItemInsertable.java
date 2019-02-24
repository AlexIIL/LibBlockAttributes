package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IItemInsertable;
import alexiil.mc.lib.attributes.item.filter.IStackFilter;

/** An {@link IItemInsertable} that always refuses to accept any inserted {@link ItemStack}. */
public enum RejectingItemInsertable implements IItemInsertable {
    NULL_INSERTABLE,

    EXTRACTOR;

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return stack;
    }

    @Override
    public IStackFilter getInsertionFilter() {
        return IStackFilter.NOTHING;
    }
}
