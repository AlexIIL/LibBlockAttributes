/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import java.util.function.Consumer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import alexiil.mc.lib.attributes.Convertible;

/** Various methods for getting {@link Reference}'s and {@link Consumer}'s from a {@link PlayerEntity}'s various
 * inventories. */
public final class PlayerInvUtil {
    private PlayerInvUtil() {}

    /** Either inserts the given item into the player's inventory or drops it in front of them. Note that this will
     * always keep a reference to the passed stack (and might modify it!) */
    public static void insertItemIntoPlayerInventory(PlayerEntity player, ItemStack stack) {
        if (player.getInventory().insertStack(stack) && stack.isEmpty()) {
            return;
        }
        player.dropItem(stack, /* PreventPlayerQuickPickup = */ false);
    }

    /** Creates a {@link Reference} to the given player's {@link PlayerInventory#getCursorStack() cursor stack}, that
     * updates the client whenever it is changed. */
    public static Reference<ItemStack> referenceGuiCursor(ServerPlayerEntity player) {
        return Reference.callable(player.getInventory()::getCursorStack, s -> {
            player.getInventory().setCursorStack(s);
            player.updateCursorStack();
        }, s -> true);
    }

    /** Creates a {@link Reference} to the what the player is currently holding in the given {@link Hand}. */
    public static Reference<ItemStack> referenceHand(PlayerEntity player, Hand hand) {
        return Reference.callable(() -> player.getStackInHand(hand), s -> player.setStackInHand(hand, s), s -> true);
    }

    /** Creates a {@link Reference} to the given {@link Slot}. If the slot is an instance of {@link StackReference}, or
     * is {@link Convertible#getAs(Object, Class)} to one, then that is returned. */
    public static Reference<ItemStack> referenceSlot(Slot slot) {
        StackReference ref = Convertible.getAs(slot, StackReference.class);
        if (ref != null) {
            return ref;
        }
        return Reference.callable(slot::getStack, slot::setStack, slot::canInsert);
    }

    /** Returns a {@link Consumer} that will call {@link #insertItemIntoPlayerInventory} for every {@link ItemStack}
     * passed to it. */
    public static Consumer<ItemStack> createPlayerInsertable(PlayerEntity player) {
        return stack -> insertItemIntoPlayerInventory(player, stack);
    }
}
