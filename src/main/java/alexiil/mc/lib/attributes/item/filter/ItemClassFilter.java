package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.ItemStack;

/** A {@link ReadableItemFilter} that only matches items of a certain {@link Class}. */
public final class ItemClassFilter implements ReadableItemFilter {

    public final Class<?> matchedClass;

    public ItemClassFilter(Class<?> matchedClass) {
        this.matchedClass = matchedClass;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return matchedClass.isInstance(stack.getItem());
    }
}
