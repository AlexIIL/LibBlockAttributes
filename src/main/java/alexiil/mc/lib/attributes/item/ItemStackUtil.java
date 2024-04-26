/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;

import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public final class ItemStackUtil {
    private ItemStackUtil() {}

    /** Checks to see if the two {@link ItemStack}'s are equal, but ignoring the {@link ItemStack#getCount() counts}. */
    public static boolean areEqualIgnoreAmounts(ItemStack a, ItemStack b) {
        if (a.isEmpty()) {
            return b.isEmpty();
        }
        if (b.isEmpty()) {
            return false;
        }
        return a.getItem() == b.getItem() && Objects.equals(a.getComponentChanges(), b.getComponentChanges());
    }

    public static NbtElement writeNbt(ItemStack stack, RegistryWrapper.WrapperLookup lookup) {
        return ItemStack.CODEC.encodeStart(lookup.getOps(NbtOps.INSTANCE), stack)
                .getPartialOrThrow(error -> saveError(stack, error));
    }

    public static ItemStack fromNbt(NbtElement nbt, RegistryWrapper.WrapperLookup lookup) {
        return fromNbt(nbt, lookup, true);
    }

    public static ItemStack fromNbt(NbtElement nbt, RegistryWrapper.WrapperLookup lookup, boolean printErrors) {
        return ItemStack.CODEC.parse(lookup.getOps(NbtOps.INSTANCE), nbt)
                .resultOrPartial(printErrors ? ItemStackUtil::loadError : s -> {})
                .orElse(ItemStack.EMPTY);
    }

    public static NbtElement writeNbtUncounted(ItemStack stack, RegistryWrapper.WrapperLookup lookup) {
        return ItemStack.UNCOUNTED_CODEC.encodeStart(lookup.getOps(NbtOps.INSTANCE), stack)
                .getPartialOrThrow(error -> saveError(stack, error));
    }

    public static ItemStack fromNbtUncounted(NbtElement nbt, RegistryWrapper.WrapperLookup lookup) {
        return fromNbtUncounted(nbt, lookup, true);
    }

    public static ItemStack fromNbtUncounted(NbtElement nbt, RegistryWrapper.WrapperLookup lookup, boolean printErrors) {
        return ItemStack.UNCOUNTED_CODEC.parse(lookup.getOps(NbtOps.INSTANCE), nbt)
                .resultOrPartial(printErrors ? ItemStackUtil::loadError : s -> {})
                .orElse(ItemStack.EMPTY);
    }

    private static @NotNull IllegalStateException saveError(ItemStack stack, String error) {
        return new IllegalStateException("Tried to save invalid item '" + stack + "': '" + error + "'");
    }

    private static void loadError(String error) {
        LibBlockAttributes.LOGGER.error("Tried to load invalid item '{}'", error);
    }
}
