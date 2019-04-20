package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** Defines an object that can have items extracted from it. */
@FunctionalInterface
public interface ItemExtractable {

    /** Attempt to extract *any* {@link ItemStack} from this that {@link ItemFilter#matches(ItemStack) matches} the
     * given {@link ItemFilter}.
     * 
     * @param filter
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @param simulation If {@link Simulation#SIMULATE} then this should return the same result that a call with
     *            {@link Simulation#ACTION} would do, but without modifying anything else.
     * @return A new, independent {@link ItemStack} that was extracted. */
    ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation);

    /** Calls {@link #attemptExtraction(ItemFilter, int, Simulation) attemptExtraction()} with an {@link ItemFilter} of
     * {@link ConstantItemFilter#ANYTHING}. */
    default ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return attemptExtraction(ConstantItemFilter.ANYTHING, maxAmount, simulation);
    }
}
