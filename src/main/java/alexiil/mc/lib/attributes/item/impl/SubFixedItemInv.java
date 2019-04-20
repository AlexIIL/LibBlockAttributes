package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;

public class SubFixedItemInv extends SubFixedItemInvView implements FixedItemInv {

    public SubFixedItemInv(FixedItemInv inv, int fromIndex, int toIndex) {
        super(inv, fromIndex, toIndex);
    }

    FixedItemInv inv() {
        return (FixedItemInv) this.inv;
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return inv().setInvStack(getInternalSlot(slot), to, simulation);
    }
}
