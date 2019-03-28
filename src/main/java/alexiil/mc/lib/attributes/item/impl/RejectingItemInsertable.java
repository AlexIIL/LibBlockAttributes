package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** An {@link ItemInsertable} that always refuses to accept any inserted {@link ItemStack}. */
public enum RejectingItemInsertable implements ItemInsertable {
    NULL,
    EXTRACTOR;

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return stack;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return ConstantItemFilter.NOTHING;
    }
}
