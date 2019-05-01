package alexiil.mc.lib.attributes.item.compat;

import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.FixedItemInv;

public class SlotFixedItemInv extends Slot {

    public final FixedItemInv inv;
    public final int slotIndex;
    private final InventoryFixedWrapper wrapper;

    private ItemStack forcedClientStackOverride = ItemStack.EMPTY;

    public SlotFixedItemInv(Container container, FixedItemInv inv, int slotIndex, int x, int y) {
        super(new InventoryFixedWrapper(inv) {
            @Override
            public boolean canPlayerUseInv(PlayerEntity player) {
                return container.canUse(player);
            }
        }, slotIndex, x, y);
        this.inv = inv;
        this.slotIndex = slotIndex;
        this.wrapper = (InventoryFixedWrapper) this.inventory;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return inv.isItemValidForSlot(slotIndex, stack);
    }

    @Override
    public int getMaxStackAmount(ItemStack stack) {
        return inv.getMaxAmount(slotIndex, stack);
    }

    @Override
    public void setStack(ItemStack stack) {
        if (wrapper.softSetInvStack(slotIndex, stack)) {
            markDirty();
            if (isClient()) {
                forcedClientStackOverride = stack;
            }
        } else {
            if (isClient()) {
                forcedClientStackOverride = stack;
            } else {
                throw new IllegalStateException("You cannot set ");
            }
        }
    }

    @Override
    public ItemStack takeStack(int amount) {
        ItemStack taken = super.takeStack(amount);
        if (isClient()) {
            forcedClientStackOverride = wrapper.getInvStack(slotIndex);
        }
        return taken;
    }

    private static boolean isClient() {
        // TODO: A more useful test to determine if this slot really is on the client and not the server.
        return true;
    }

    @Override
    public ItemStack getStack() {
        if (!forcedClientStackOverride.isEmpty()) {
            return forcedClientStackOverride;
        }
        return super.getStack();
    }
}
