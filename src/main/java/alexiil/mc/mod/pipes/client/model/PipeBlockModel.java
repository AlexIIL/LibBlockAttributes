package alexiil.mc.mod.pipes.client.model;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

import alexiil.mc.mod.pipes.blocks.BlockPipe;
import alexiil.mc.mod.pipes.blocks.TilePipe;
import alexiil.mc.mod.pipes.blocks.TilePipe.PipeBlockModelState;

public class PipeBlockModel extends PerspAwareModelBase implements IWorldDependentModel {

    public PipeBlockModel(BlockPipe pipeBlock) {
        super(ImmutableList.of(), PipeBaseModelGenStandard.getCenterSprite(pipeBlock));
    }

    @Override
    public BakedModel getRealModel(ExtendedBlockView view, BlockPos pos, BlockState state) {
        BlockEntity tile = view.getBlockEntity(pos);
        if (tile instanceof TilePipe) {
            return bakeModel(((TilePipe) tile).blockModelState);
        }
        return bakeModel(null);
    }

    private BakedModel bakeModel(@Nullable PipeBlockModelState state) {
        if (state == null) {
            state = new PipeBlockModelState(null, (byte) 0);
        }
        List<BakedQuad> quads = PipeBaseModelGenStandard.generateCutout(state);
        return new PerspAwareModelBase(quads, quads.isEmpty() ? getSprite() : quads.get(0).getSprite());
    }
}
