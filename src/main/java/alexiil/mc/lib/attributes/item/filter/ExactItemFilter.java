package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** An {@link ItemFilter} that only matches on a single {@link Item}. */
public final class ExactItemFilter implements ReadableItemFilter {

    public final Item item;

    public ExactItemFilter(Item item) {
        this.item = item;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.getItem() == item;
    }
}
