package alexiil.mc.mod.pipes.client.model;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public class PerspAwareModelBase implements BakedModel {
    private final ImmutableList<BakedQuad> quads;
    private final Sprite particle;

    public PerspAwareModelBase(List<BakedQuad> quads, Sprite particle) {
        this.quads = quads == null ? ImmutableList.of() : ImmutableList.copyOf(quads);
        this.particle = particle != null ? particle : MissingSprite.getMissingSprite();
    }

    public static List<BakedQuad> missingModel() {
        BakedModel model =
            MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelManager().getMissingModel();
        return model.getQuads(Blocks.AIR.getDefaultState(), null, new Random());
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
        return side == null ? quads : ImmutableList.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean hasDepthInGui() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getSprite() {
        return particle;
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelTransformation.NONE;
    }

    @Override
    public ModelItemPropertyOverrideList getItemPropertyOverrides() {
        return ModelItemPropertyOverrideList.EMPTY;
    }
}
