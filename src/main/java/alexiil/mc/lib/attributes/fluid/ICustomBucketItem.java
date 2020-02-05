/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;

/** Items that extend {@link BucketItem} should implement this interface to ensure that LBA gets the correct
 * {@link ItemStack} from filling this with a fluid. */
public interface ICustomBucketItem {
    ItemStack getFilledBucket(Fluid fluid);
}
