package alexiil.mc.lib.attributes.item.impl;

import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.IItemFilter;

public final class CombinedItemExtractable implements IItemExtractable {

    private final List<? extends IItemExtractable> list;

    public CombinedItemExtractable(List<? extends IItemExtractable> list) {
        this.list = list;
    }

    @Override
    public ItemStack attemptExtraction(IItemFilter filter, int maxAmount, Simulation simulation) {
        if (maxAmount < 0) {
            throw new IllegalArgumentException("maxCount cannot be negative! (was " + maxAmount + ")");
        }
        ItemStack extracted = ItemStack.EMPTY;
        for (IItemExtractable extractable : list) {
            if (extracted.isEmpty()) {
                extracted = extractable.attemptExtraction(filter, maxAmount, simulation);
                if (extracted.isEmpty()) {
                    continue;
                }
                if (extracted.getAmount() >= maxAmount) {
                    return extracted;
                }
                filter = new ExactItemStackFilter(extracted);
            } else {
                int newMaxCount = maxAmount - extracted.getAmount();
                ItemStack additional = extractable.attemptExtraction(filter, newMaxCount, simulation);
                if (additional.isEmpty()) {
                    continue;
                }
                if (!ItemStackUtil.areEqualIgnoreAmounts(additional, extracted)) {
                    throw new IllegalStateException("bad IItemExtractable " + extractable.getClass().getName());
                }
                extracted.addAmount(additional.getAmount());
                if (extracted.getAmount() >= maxAmount) {
                    return extracted;
                }
            }
        }
        return extracted;
    }
}
