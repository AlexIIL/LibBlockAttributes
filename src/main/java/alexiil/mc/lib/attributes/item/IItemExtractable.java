package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.IStackFilter;
import alexiil.mc.lib.attributes.item.impl.CombinedItemExtractable;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;

/** Defines an object that can have items extracted from it. */
public interface IItemExtractable {

    public static final CombinableAttribute<IItemExtractable> ATTRIBUTE_EXTRACTABLE = new CombinableAttribute<>(
        IItemExtractable.class, EmptyItemExtractable.NULL_EXTRACTABLE, CombinedItemExtractable::new);

    /** Attempt to extract *any* {@link ItemStack} from this that {@link IStackFilter#matches(ItemStack) matches} the
     * given {@link IStackFilter}.
     * 
     * @param filter
     * @param maxAmount The maximum number of items that can be extracted. Negative numbers throw an exception.
     * @param simulation If {@link Simulation#SIMULATE} then this should return the same result that a call with
     *            {@link Simulation#ACTION} would do, but without modifying anything else.
     * @return A new, independent {@link ItemStack} that was extracted. */
    ItemStack attemptExtraction(IStackFilter filter, int maxAmount, Simulation simulation);

    /** Calls {@link #attemptExtraction(IStackFilter, int, Simulation) attemptExtraction()} with an {@link IStackFilter}
     * of {@link IStackFilter#ANY_STACK}. */
    default ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return attemptExtraction(IStackFilter.ANY_STACK, maxAmount, simulation);
    }
}
