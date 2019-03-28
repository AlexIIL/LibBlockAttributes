package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** Defines an object that can have items inserted into it. */
public interface ItemInsertable {

    /** Inserts the given stack into this insertable, and returns the excess.
     * 
     * @param stack The incoming stack. Must not be modified by this call.
     * @param simulation If {@link Simulation#SIMULATE} then this shouldn't modify anything.
     * @return the excess {@link ItemStack} that wasn't accepted. This will be independent of this insertable, however
     *         it might be the given stack instead of a completely new object. */
    ItemStack attemptInsertion(ItemStack stack, Simulation simulation);

    /** Returns an {@link ItemFilter} to determine if {@link #attemptInsertion(ItemStack, Simulation)} will accept a
     * stack. The default implementation is a call to {@link #attemptInsertion(ItemStack, Simulation)
     * attemptInsertion}(stack, {@link Simulation#SIMULATE}), and it is only useful to override this if the resulting
     * filter contains information that might be usable by the caller.
     * 
     * @return A filter to determine if {@link #attemptInsertion(ItemStack, Simulation)} will accept the entirety of a
     *         given stack. */
    default ItemFilter getInsertionFilter() {
        return stack -> {
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("You should never test an IItemFilter with an empty stack!");
            }
            ItemStack leftover = attemptInsertion(stack, Simulation.SIMULATE);
            return leftover.isEmpty() || leftover.getAmount() < stack.getAmount();
        };
    }
}
