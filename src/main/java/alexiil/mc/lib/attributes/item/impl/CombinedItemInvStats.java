package alexiil.mc.lib.attributes.item.impl;

import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.ItemInvStats;
import alexiil.mc.lib.attributes.item.ItemStackCollections;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

public class CombinedItemInvStats implements ItemInvStats {

    private final List<? extends ItemInvStats> statistics;

    public CombinedItemInvStats(List<? extends ItemInvStats> statistics) {
        this.statistics = statistics;
    }

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        int amount = 0;
        int spaceAddable = 0;
        int spaceTotal = 0;
        for (ItemInvStats stats : statistics) {
            ItemInvStatistic stat = stats.getStatistics(filter);
            amount += stat.amount;
            spaceAddable += stat.spaceAddable;
            spaceTotal += stat.spaceTotal;
        }
        return new ItemInvStatistic(filter, amount, spaceAddable, spaceTotal);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        Set<ItemStack> set = ItemStackCollections.set();
        for (ItemInvStats stats : statistics) {
            set.addAll(stats.getStoredStacks());
        }
        return set;
    }
}
