package alexiil.mc.lib.attributes.item.compat;

import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.impl.PartialInventoryFixedWrapper;

public class SlotFixedItemInv extends Slot {

    public final FixedItemInv inv;
    public final int slotIndex;

    public SlotFixedItemInv(Container container, FixedItemInv inv, int slotIndex, int x, int y) {
        super(new PartialInventoryFixedWrapper(inv) {
            @Override
            public boolean canPlayerUseInv(PlayerEntity player) {
                return container.canUse(player);
            }
        }, slotIndex, x, y);
        this.inv = inv;
        this.slotIndex = slotIndex;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return inv.isItemValidForSlot(slotIndex, stack);
    }

    @Override
    public int getMaxStackAmount(ItemStack stack) {
        return inv.getMaxAmount(slotIndex, stack);
    }
}
