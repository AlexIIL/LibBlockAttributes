/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
/** The base package for {@link net.minecraft.fluid.Fluid fluid} management.
 * <p>
 * As vanilla minecraft doesn't use fluids as much as items there are a few key differences between how this package
 * works verses {@link alexiil.mc.lib.attributes.item}:
 * <ul>
 * <li>Instead of {@link net.minecraft.item.ItemStack ItemStack} we have
 * {@link alexiil.mc.lib.attributes.fluid.volume.FluidVolume FluidVolume} (with an amount) and
 * {@link alexiil.mc.lib.attributes.fluid.volume.FluidKey FluidKey} (without an amount)</li>
 * <li>Slots are called "tanks"</li>
 * <li>Tanks/Slots do not have a pre-defined maximum amount (and neither do fluids themselves as that depends wholly on
 * the container).</li>
 * <li>The units for fluids are fractional amounts based on 1+0/1 being equal to 1 bucket, stored in
 * {@link alexiil.mc.lib.attributes.fluid.amount.FluidAmount}.</li>
 * </ul>
 */
package alexiil.mc.lib.attributes.fluid;
