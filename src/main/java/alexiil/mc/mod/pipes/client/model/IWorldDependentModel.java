package alexiil.mc.mod.pipes.client.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

public interface IWorldDependentModel extends BakedModel {
    BakedModel getRealModel(ExtendedBlockView view, BlockPos pos, BlockState state);
}
