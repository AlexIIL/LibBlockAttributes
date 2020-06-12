/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;

final class NullAttributeAdder<T> implements CustomAttributeAdder<T>, ItemAttributeAdder<T> {
    private NullAttributeAdder() {}

    private static final NullAttributeAdder<Object> INSTANCE = new NullAttributeAdder<>();

    public static <T> NullAttributeAdder<T> get() {
        // Safe because we don't actually do anything with the type.
        return (NullAttributeAdder<T>) INSTANCE;
    }

    @Override
    public void addAll(Reference<ItemStack> stack, LimitedConsumer<ItemStack> excess, ItemAttributeList<T> to) {
        // NO-OP
    }

    @Override
    public void addAll(World world, BlockPos pos, BlockState state, AttributeList<T> to) {
        // NO-OP
    }
}
