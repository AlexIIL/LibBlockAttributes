/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.item;

import org.junit.Assert;
import org.junit.Test;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;

import alexiil.mc.lib.attributes.VanillaSetupBaseTester;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.Ref;

public class FluidContainerTester extends VanillaSetupBaseTester {

    @Test
    public void testFilling() {
        Ref<ItemStack> ref = new Ref<>(new ItemStack(Items.GLASS_BOTTLE));
        FluidVolume toInsert = FluidKeys.get(Potions.HEALING).withAmount(FluidAmount.BOTTLE);
        FluidVolume excess = FluidAttributes.INSERTABLE.get(ref).insert(toInsert);
        Assert.assertEquals(FluidVolumeUtil.EMPTY, excess);
        Assert.assertEquals(Items.POTION, ref.get().getItem());
    }
}
