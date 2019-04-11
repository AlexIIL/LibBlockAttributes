package alexiil.mc.lib.attributes.misc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;

import alexiil.mc.lib.attributes.fluid.volume.PotionFluidKey;

public class ClientInit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientSpriteRegistryCallback.registerBlockAtlas((atlasTexture, registry) -> {
            registry.register(PotionFluidKey.POTION_TEXTURE);
        });
    }
}
