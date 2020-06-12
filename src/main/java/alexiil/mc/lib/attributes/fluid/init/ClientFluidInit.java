/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.init;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;

import alexiil.mc.lib.attributes.fluid.volume.PotionFluidKey;

public class ClientFluidInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEX).register((atlasTexture, registry) -> {
            registry.register(PotionFluidKey.POTION_TEXTURE);
        });

        LbaFluidProxy.MC_TOOLTIPS_ADVANCED = () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                return false;
            }
            return mc.options == null ? false : mc.options.advancedItemTooltips;
        };
    }
}
