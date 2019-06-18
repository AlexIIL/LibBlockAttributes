package alexiil.mc.lib.attributes.item.compat;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.ItemStackUtil;

/** An {@link FixedItemInv} that wraps a vanilla {@link Inventory}. */
public class FixedInventoryVanillaWrapper extends FixedInventoryViewVanillaWrapper implements FixedItemInv {

    public FixedInventoryVanillaWrapper(Inventory inv) {
        super(inv);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        boolean allowed = false;
        if (to.isEmpty()) {
            if (inv instanceof SidedInventory) {
                SidedInventory sided = (SidedInventory) inv;
                // TODO: pass the direction into a new subclass for SidedInventory.
                // allowed = sided.canExtractInvStack(slot, var2, var3);
            } else {
                allowed = true;
            }
        } else {
            ItemStack current = getInvStack(slot);
            if (
                !current.isEmpty()
                && current.getAmount() > to.getAmount()
                && ItemStackUtil.areEqualIgnoreAmounts(to, current)
            ) {
                allowed = true;
            } else if (isItemValidForSlot(slot, to) && to.getAmount() <= getMaxAmount(slot, to)) {
                allowed = true;
            }
        }
        if (allowed) {
            if (simulation == Simulation.ACTION) {
                inv.setInvStack(slot, to);
            }
            return true;
        }
        return false;
    }
}
