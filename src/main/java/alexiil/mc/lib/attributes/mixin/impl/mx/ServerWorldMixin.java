/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.mixin.impl.mx;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;

import alexiil.mc.lib.attributes.mixin.api.UnloadableBlockEntity;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(at = @At("HEAD"), method = "unloadEntities(Lnet/minecraft/world/chunk/WorldChunk;)V")
    private void lba_unloadBlockEntities(WorldChunk chunk, CallbackInfo ci) {
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof UnloadableBlockEntity) {
                ((UnloadableBlockEntity) be).onChunkUnload();
            }
        }
    }
}
