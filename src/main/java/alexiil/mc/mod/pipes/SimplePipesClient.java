package alexiil.mc.mod.pipes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.render.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;

import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.pipes.blocks.TilePipe;
import alexiil.mc.mod.pipes.client.model.SimplePipeModels;
import alexiil.mc.mod.pipes.client.render.PipeBlockEntityRenderer;

public class SimplePipesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(SimplePipeModels::createVariantProvider);
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(SimplePipeModels::createResourceProvider);
        ClientSpriteRegistryCallback.registerBlockAtlas(this::registerSprites);
        BlockEntityRendererRegistry.INSTANCE.register(TilePipe.class, new PipeBlockEntityRenderer());
    }

    private void registerSprites(SpriteAtlasTexture atlasTexture, ClientSpriteRegistryCallback.Registry registry) {
        registry.register(new Identifier(SimplePipes.MODID, "pipe_wooden_clear"));
        registry.register(new Identifier(SimplePipes.MODID, "pipe_wooden_filled"));
        registry.register(new Identifier(SimplePipes.MODID, "pipe_stone"));
        registry.register(new Identifier(SimplePipes.MODID, "pipe_iron_clear"));
        registry.register(new Identifier(SimplePipes.MODID, "pipe_iron_filled"));
    }
}
