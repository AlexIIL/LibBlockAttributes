package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;

public class MappedFixedItemInv extends MappedFixedItemInvView implements FixedItemInv {

    public MappedFixedItemInv(FixedItemInv inv, int[] slots) {
        super(inv, slots);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return ((FixedItemInv) inv).setInvStack(getInternalSlot(slot), to, simulation);
    }
}
