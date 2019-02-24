package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.IItemInsertable;
import alexiil.mc.lib.attributes.item.filter.IStackFilter;

/** An {@link IItemExtractable} that never returns any items from
 * {@link #attemptExtraction(IStackFilter, int, Simulation)}. */
public enum EmptyItemExtractable implements IItemExtractable {
    /** An {@link IItemExtractable} that should be treated as equal to null in all circumstances - that is any checks
     * that depend on an object being extractable should be considered FALSE for this instance. */
    NULL_EXTRACTABLE,

    /** An {@link IItemExtractable} that informs callers that it will push items into a nearby {@link IItemInsertable},
     * but doesn't expose any other item based attributes.
     * <p>
     * The buildcraft quarry is a good example of this - it doesn't have any inventory slots itself and it pushes items
     * out of it as it mines them from the world, but item pipes should still connect to it so that it can insert into
     * them. */
    SUPPLIER;

    @Override
    public ItemStack attemptExtraction(IStackFilter filter, int maxCount, Simulation simulation) {
        return ItemStack.EMPTY;
    }
}
