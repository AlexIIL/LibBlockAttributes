package alexiil.mc.lib.attributes.fluid.volume;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;

public class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientSpriteRegistryCallback.registerBlockAtlas((atlasTexture, registry) -> {
            registry.register(PotionFluidKey.POTION_TEXTURE);
        });
    }
}
