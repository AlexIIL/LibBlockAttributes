/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import javax.annotation.Nullable;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;

import alexiil.mc.lib.attributes.Convertible;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryViewVanillaWrapper;
import alexiil.mc.lib.attributes.item.compat.FixedSidedInventoryVanillaWrapper;

/** An {@link Object} that wraps an object of a different type into some other type. This is generally the inverse of
 * {@link Convertible#convertTo(Class)}. */
public interface OpenWrapper {

    /** @return The object that this wraps, or null if the wrapped object cannot be converted cleanly without exposing
     *         additional details.
     *         <p>
     *         For example LBA's {@link Inventory} to {@link FixedItemInvView} wrapper
     *         ({@link FixedInventoryViewVanillaWrapper}) implements this to return the {@link Inventory}, but the
     *         {@link SidedInventory} variant ({@link FixedSidedInventoryVanillaWrapper}) returns null, as it is not
     *         possible to cleanly open the original inventory without exposing additional slots. */
    @Nullable
    Object getWrapped();
}
