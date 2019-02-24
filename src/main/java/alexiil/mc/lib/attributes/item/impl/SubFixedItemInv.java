package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IFixedItemInv;

public class SubFixedItemInv<InvType extends IFixedItemInv> extends SubFixedItemInvView<InvType>
    implements IFixedItemInv {

    public SubFixedItemInv(InvType inv, int fromIndex, int toIndex) {
        super(inv, fromIndex, toIndex);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return inv.setInvStack(getInternalSlot(slot), to, simulation);
    }
}
