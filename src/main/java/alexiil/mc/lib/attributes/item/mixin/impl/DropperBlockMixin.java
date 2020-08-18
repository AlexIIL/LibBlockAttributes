/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.block.DropperBlock;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.BlockPos;

import alexiil.mc.lib.attributes.item.mixin.HopperHooks;

@Mixin(DropperBlock.class)
public class DropperBlockMixin {

    private static final String DISPENSER_BLOCK_ENTITY = "Lnet/minecraft/block/entity/DispenserBlockEntity;";

    @Inject(
        method = "dispense", at = @At(value = "INVOKE_ASSIGN", target = DISPENSER_BLOCK_ENTITY + "chooseNonEmptySlot()I"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD
    )
    void dispenseIntoLba(
        ServerWorld serverWorld, BlockPos pos, CallbackInfo ci, BlockPointerImpl pointer, DispenserBlockEntity be,
        int index
    ) {
        ActionResult result = HopperHooks.tryDispense(be, index);
        if (result != ActionResult.PASS) {
            ci.cancel();
        }
    }
}
