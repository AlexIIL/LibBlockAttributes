package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.DelegatingGroupedItemInv;

public interface LimitedGroupedItemInv extends GroupedItemInv {

    /** Marks this object as final, and disallows any further changes to this. If this is called then
     * {@link #asUnmodifiable()} will return this object.
     * 
     * @return this. */
    LimitedGroupedItemInv markFinal();

    /** Creates a copy of this {@link LimitedGroupedItemInv} (with the same backing inventory and the same rules). */
    LimitedGroupedItemInv copy();

    /** Completely clears all rules currently imposed.
     * 
     * @return This. */
    default LimitedGroupedItemInv reset() {
        getAllRule().reset();
        return this;
    }

    /** @return A new {@link GroupedItemInv} with the current rules of this, but that cannot be modified. */
    default GroupedItemInv asUnmodifiable() {
        return new DelegatingGroupedItemInv(this);
    }

    /** @return An {@link ItemLimitRule} that applies to everything. */
    default ItemLimitRule getAllRule() {
        return getRule(ConstantItemFilter.ANYTHING);
    }

    /** @return A rule for the specific {@link ItemStack} given. */
    default ItemLimitRule getRule(ItemStack stack) {
        return getRule(new ExactItemStackFilter(stack));
    }

    /** @return An {@link ItemLimitRule} that applies to every item that matches the given filter. */
    ItemLimitRule getRule(ItemFilter filter);

    /** A rule that is applied once for every {@link ItemStack} that matches the {@link ItemFilter} that was used in
     * {@link LimitedGroupedItemInv#getRule(ItemFilter)}. */
    public interface ItemLimitRule {

        /** Completely disallows insertion and extraction for this {@link ItemLimitRule}. */
        default ItemLimitRule disallowTransfer() {
            return disallowInsertion().disallowExtraction();
        }

        /** Clears all limitations for this current rule. */
        default ItemLimitRule reset() {
            return allowExtraction().allowInsertion();
        }

        /** Disallows insertion for this {@link ItemLimitRule}. */
        default ItemLimitRule disallowInsertion() {
            return limitInsertionCount(0);
        }

        /** Resets any insertion limitations previously imposed by this {@link ItemLimitRule}. */
        default ItemLimitRule allowInsertion() {
            return limitInsertionCount(-1);
        }

        /** Limits the number of items that can be inserted (in total) to the given count.
         * 
         * @param max The maximum. A value less than 0 will reset this back to no limits.
         * @return this. */
        ItemLimitRule limitInsertionCount(int max);

        /** Completely disallows extraction of items.
         * 
         * @return this. */
        default ItemLimitRule disallowExtraction() {
            return setMinimum(Integer.MAX_VALUE);
        }

        /** Stops disallowing extraction of items.
         * 
         * @return this. */
        default ItemLimitRule allowExtraction() {
            return setMinimum(0);
        }

        /** Limits the number of items that can be extracted to ensure that the inventory cannot have an amount below
         * the given value. (This of course has no effect on the underlying inventory, so it is always possible for the
         * underlying inventory to be modified to contain less than the given amount).
         * 
         * @param min The minimum number of items. A value of 0 removes the rule for this {@link ItemFilter}.
         * @return this. */
        ItemLimitRule setMinimum(int min);
    }
}
