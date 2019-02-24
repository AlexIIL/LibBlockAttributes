package alexiil.mc.mod.pipes.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

import alexiil.mc.mod.pipes.client.model.IWorldDependentModel;

@Mixin(BlockModelRenderer.class)
@Debug
public class BlockModelRendererMixin {

    @ModifyArg(method = { "tesselate" }, at = @At(value = "INVOKE", target = "tesselateSmooth"))
    public BakedModel tesselate_get_model_smooth(ExtendedBlockView view, BakedModel model, BlockState state,
        BlockPos pos, BufferBuilder builder, boolean b, Random r, long l) {
        return replaceModel(view, pos, state, model);
    }

    @ModifyArg(method = { "tesselate" }, at = @At(value = "INVOKE", target = "tesselateFlat"))
    public BakedModel tesselate_get_model_flat(ExtendedBlockView view, BakedModel model, BlockState state, BlockPos pos,
        BufferBuilder builder, boolean b, Random r, long l) {
        return replaceModel(view, pos, state, model);
    }

    @Unique
    private static BakedModel replaceModel(ExtendedBlockView view, BlockPos pos, BlockState state, BakedModel model) {

        if (model instanceof IWorldDependentModel) {
            model = ((IWorldDependentModel) model).getRealModel(view, pos, state);
        }

        return model;
    }
}
