package alexiil.mc.mod.pipes.client.model;

import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.fabricmc.fabric.api.client.model.ModelVariantProvider;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import alexiil.mc.mod.pipes.SimplePipes;
import alexiil.mc.mod.pipes.blocks.SimplePipeBlocks;

public class SimplePipeModels {

    public static ModelResourceProvider createResourceProvider(ResourceManager manager) {
        return (Identifier resourceId, ModelProviderContext context) -> {
            if ("curved_rails".equals(resourceId.getNamespace())) {
                System.out.println("resource: " + resourceId);
            }
            return null;
        };
    }

    public static ModelVariantProvider createVariantProvider(ResourceManager manager) {
        return (ModelIdentifier resourceId, ModelProviderContext context) -> {
            BakedModel baked = getModel(manager, resourceId, context);
            if (baked != null) {
                return new PreBakedModel(baked);
            }
            return null;
        };
    }

    private static BakedModel getModel(ResourceManager manager, ModelIdentifier resourceId,
        ModelProviderContext context) {
        if ("inventory".equals(resourceId.getVariant())) {
            return null;
        }

        switch (resourceId.getNamespace()) {
            case SimplePipes.MODID: {

                switch (resourceId.getPath()) {

                    case "pipe_wooden":
                        return new PipeBlockModel(SimplePipeBlocks.WOODEN_PIPE);
                    case "pipe_stone":
                        return new PipeBlockModel(SimplePipeBlocks.STONE_PIPE);
                    case "pipe_iron":
                        return new PipeBlockModel(SimplePipeBlocks.IRON_PIPE);

                    default:
                        System.out.println(resourceId);
                        return null;
                }
            }
            default: {
                return null;
            }
        }
    }
}
