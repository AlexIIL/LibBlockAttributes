package alexiil.mc.lib.attributes.fluid.init;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;

import net.minecraft.client.texture.SpriteAtlasTexture;

import alexiil.mc.lib.attributes.fluid.volume.PotionFluidKey;

public class ClientFluidInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEX).register((atlasTexture, registry) -> {
            registry.register(PotionFluidKey.POTION_TEXTURE);
        });
    }
}
