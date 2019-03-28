package alexiil.mc.lib.attributes.item.impl;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.ItemInvStats;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** {@link ItemInvStats} for an empty inventory. */
public enum EmptyItemInvStats implements ItemInvStats {
    INSTANCE;

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        return new ItemInvStatistic(filter, 0, 0, 0);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        return Collections.emptySet();
    }
}
