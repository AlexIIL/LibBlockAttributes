package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.CombinableAttribute;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;
import alexiil.mc.lib.attributes.item.impl.CombinedItemInsertable;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;

/** Defines an object that can have items inserted into it. */
public interface IItemInsertable {

    /** The {@link Attribute} for {@link IItemInsertable}. */
    public static final CombinableAttribute<IItemInsertable> ATTRIBUTE_INSERTABLE = new CombinableAttribute<>(
        IItemInsertable.class, RejectingItemInsertable.NULL_INSERTABLE, CombinedItemInsertable::new);

    /** Inserts the given stack into this insertable, and returns the excess.
     * 
     * @param stack The incoming stack. Must not be modified by this call.
     * @param simulation If {@link Simulation#SIMULATE} then this shouldn't modify anything.
     * @return the excess {@link ItemStack} that wasn't accepted. This will be independent of this insertable, however
     *         it might be the given stack instead of a completly new object. */
    ItemStack attemptInsertion(ItemStack stack, Simulation simulation);

    /** Returns an {@link IItemFilter} to determine if {@link #attemptInsertion(ItemStack, Simulation)} will accept a
     * stack. The default implementation is a call to {@link #attemptInsertion(ItemStack, Simulation)
     * attemptInsertion}(stack, {@link Simulation#SIMULATE}), and it is only useful to override this if the resulting
     * filter contains information that might be usable by the caller.
     * 
     * @return A filter to determine if {@link #attemptInsertion(ItemStack, Simulation)} will accept the entirety of a
     *         given stack. */
    default IItemFilter getInsertionFilter() {
        return stack -> attemptInsertion(stack, Simulation.SIMULATE).isEmpty();
    }
}
