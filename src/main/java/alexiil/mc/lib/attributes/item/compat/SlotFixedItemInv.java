/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import alexiil.mc.lib.attributes.Convertible;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.misc.StackReference;

public class SlotFixedItemInv extends Slot implements Convertible {

    public final FixedItemInv inv;
    public final int slotIndex;
    public final boolean server;
    private final InventoryFixedWrapper serverWrapper;
    private StackReference realRef;

    public SlotFixedItemInv(ScreenHandler container, FixedItemInv inv, boolean server, int slotIndex, int x, int y) {
        super(createInv(container, inv, server), server ? slotIndex : 0, x, y);
        this.inv = inv;
        this.slotIndex = slotIndex;
        this.server = server;
        if (server) {
            this.serverWrapper = (InventoryFixedWrapper) this.inventory;
            this.realRef = inv.getSlot(slotIndex);
        } else {
            this.serverWrapper = null;
        }
    }

    private static Inventory createInv(ScreenHandler container, FixedItemInv inv, boolean server) {
        if (server) {
            return new InventoryFixedWrapper(inv) {
                @Override
                public boolean canPlayerUse(PlayerEntity player) {
                    return container.canUse(player);
                }
            };
        } else {
            return new SimpleInventory(1) {
                @Override
                public boolean canPlayerUse(PlayerEntity player) {
                    return container.canUse(player);
                }
            };
        }
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
        if (server) {
            if (serverWrapper.softSetInvStack(slotIndex, stack)) {
                markDirty();
            }
        } else {
            super.setStack(stack);
        }
    }

    @Override
    public <T> T convertTo(Class<T> otherType) {
        if (StackReference.class.isAssignableFrom(otherType)) {
            if (realRef == null) {
                realRef = new StackReference() {
                    @Override
                    public boolean set(ItemStack stack) {
                        if (isValid(stack)) {
                            SlotFixedItemInv.super.setStack(stack);
                            return false;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public boolean isValid(ItemStack stack) {
                        return inv.isItemValidForSlot(slotIndex, stack);
                    }

                    @Override
                    public ItemStack get() {
                        return getStack();
                    }
                };
            }
            return otherType.cast(realRef);
        }
        return null;
    }
}
