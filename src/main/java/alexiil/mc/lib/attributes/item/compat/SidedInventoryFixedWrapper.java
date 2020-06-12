/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat;

import java.util.Arrays;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.ItemInvUtil;

/** A {@link SidedInventory} that wraps an {@link FixedItemInv}.
 * <p>
 * One of the {@link Inventory} methods must be overridden by subclasses however:
 * {@link Inventory#canPlayerUse(PlayerEntity)}. */
public abstract class SidedInventoryFixedWrapper extends InventoryFixedWrapper implements SidedInventory {

    /** Unlike the int[][] passed into constructors this is always a length 7 array. */
    private int[][] availableSlots;

    /** Creates a {@link SidedInventoryFixedWrapper} with all of it's slots exposed in every direction. */
    public SidedInventoryFixedWrapper(FixedItemInv inv) {
        super(inv);
        resetSlotsToAll();
    }

    private static int[][] createFullArray(FixedItemInv inv) {
        int[] a = createSingleArray(inv);
        return new int[][] { a, a, a, a, a, a, a };
    }

    private static int[] createSingleArray(FixedItemInv inv) {
        int[] a = new int[inv.getSlotCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        return a;
    }

    /** @param inv The {@link FixedItemInv} to wrap.
     * @param slotMap The slots to map. There are 3 different possible lengths you can give for this:
     *            <ol>
     *            <li>Null or 0: Every slot is available from every direction.</li>
     *            <li>6: Every slot is available from the null direction, and the other slots are direct maps for a
     *            Direction.ordinal().</li>
     *            <li>7: index 0-5 are for their respective {@link Direction#ordinal()}, and index 6 is for the null
     *            direction.
     *            </ol>
     */
    public SidedInventoryFixedWrapper(FixedItemInv inv, int[][] slotMap) {
        super(inv);
        resetSlotsTo(slotMap);
    }

    /** Discards the current {@link #availableSlots} array and replaces it with one that exposes all slots in every
     * direction. */
    protected void resetSlotsToAll() {
        availableSlots = createFullArray(inv);
    }

    /** Discards the current {@link #availableSlots} array and replaces it with the given slot map.
     * 
     * @param slotMap The slots to map. There are 3 different possible lengths you can give for this:
     *            <ol>
     *            <li>Null or 0: Every slot is available from every direction.</li>
     *            <li>6: Every slot is available from the null direction, and the other slots are direct maps for a
     *            Direction.ordinal().</li>
     *            <li>7: index 0-5 are for their respective {@link Direction#ordinal()}, and index 6 is for the null
     *            direction.
     *            </ol>
     */
    protected void resetSlotsTo(int[][] slotMap) {
        if (slotMap != null && slotMap.length != 0) {
            if (slotMap.length == 6 || slotMap.length == 7) {
                for (int[] arr : slotMap) {
                    for (int slot : arr) {
                        if (slot < 0 || slot >= inv.getSlotCount()) {
                            throw new IllegalArgumentException(
                                "Invalid slot index (" + slot + ", max = " + (inv.getSlotCount() - 1)
                                    + ") in the slot map " + Arrays.deepToString(slotMap)
                            );
                        }
                    }
                }
                if (slotMap.length == 6) {
                    availableSlots = new int[][] { slotMap[0], slotMap[1], slotMap[2], slotMap[3], slotMap[4],
                        slotMap[5], createSingleArray(inv) };
                } else {
                    availableSlots = slotMap;
                }
            } else {
                throw new IllegalArgumentException(
                    "The given slot map was of an invalid length (" + slotMap.length
                        + ")!\n\tIt must be either 0, 6, or 7!"
                );
            }
        } else {
            availableSlots = createFullArray(inv);
        }
    }

    @Override
    public int[] getAvailableSlots(Direction dir) {
        return copy(getSlotsInternal(dir));
    }

    private int[] getSlotsInternal(Direction dir) {
        return availableSlots[dir == null ? 6 : dir.ordinal()];
    }

    private static int[] copy(int[] array) {
        return Arrays.copyOf(array, array.length);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        int[] slots = getSlotsInternal(dir);
        for (int s : slots) {
            if (s == slot) {
                return ItemInvUtil.insertSingle(inv, s, stack, Simulation.SIMULATE).isEmpty();
            }
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        int[] slots = getSlotsInternal(dir);
        for (int s : slots) {
            if (s == slot) {
                return !ItemInvUtil.extractSingle(inv, slot, null, ItemStack.EMPTY, 1, Simulation.SIMULATE).isEmpty();
            }
        }
        return false;
    }
}
