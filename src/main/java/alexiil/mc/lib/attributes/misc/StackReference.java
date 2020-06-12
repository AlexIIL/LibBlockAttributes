/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Specialist {@link Reference} interface for {@link ItemStack}s. NOTE: You <em>cannot</em> assume that all
 * {@link Reference}s for {@link ItemStack}s will implement this, as this is only provided in the rare case that someone
 * needs to perform an "instance of" check.
 * <p>
 * (For example {@link PlayerInvUtil#referenceSlot(net.minecraft.screen.slot.Slot)} uses this to attempt to convert a
 * {@link Slot} into a {@link StackReference} directly, rather than generate a potentially-inaccurate reference from the
 * slot. */
public interface StackReference extends Reference<ItemStack> {}
