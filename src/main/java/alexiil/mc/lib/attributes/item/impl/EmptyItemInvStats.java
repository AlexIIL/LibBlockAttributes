package alexiil.mc.lib.attributes.item.impl;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.IItemInvStats;
import alexiil.mc.lib.attributes.item.filter.IStackFilter;

/** {@link IItemInvStats} for an empty inventory. */
public enum EmptyItemInvStats implements IItemInvStats {
    INSTANCE;

    @Override
    public ItemInvStatistic getStatistics(IStackFilter filter) {
        return new ItemInvStatistic(filter, 0, 0, 0);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        return Collections.emptySet();
    }
}
