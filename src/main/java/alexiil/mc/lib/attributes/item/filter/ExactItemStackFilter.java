package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.ItemStackUtil;

/** An {@link IItemFilter} that only matches on a single {@link ItemStack}. */
public final class ExactItemStackFilter implements IReadableItemFilter {

    public final ItemStack stack;

    public ExactItemStackFilter(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemStackUtil.areEqualIgnoreAmounts(this.stack, stack);
    }
}
