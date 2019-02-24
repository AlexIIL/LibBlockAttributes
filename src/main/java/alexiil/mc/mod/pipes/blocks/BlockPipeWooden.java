package alexiil.mc.mod.pipes.blocks;

import net.minecraft.world.BlockView;

public class BlockPipeWooden extends BlockPipeSided {

    public BlockPipeWooden(Settings settings) {
        super(settings);
    }

    @Override
    public TilePipeSided createBlockEntity(BlockView var1) {
        return new TilePipeWood();
    }
}
