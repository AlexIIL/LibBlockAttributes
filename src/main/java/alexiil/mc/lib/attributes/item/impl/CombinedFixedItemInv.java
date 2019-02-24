package alexiil.mc.lib.attributes.item.impl;

import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IFixedItemInv;

/** An {@link IFixedItemInv} that delegates to a list of them instead of storing items directly. */
public class CombinedFixedItemInv<InvType extends IFixedItemInv> extends CombinedFixedItemInvView<InvType>
    implements IFixedItemInv {

    public CombinedFixedItemInv(List<? extends InvType> views) {
        super(views);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return getInv(slot).setInvStack(getSubSlot(slot), to, simulation);
    }
}
