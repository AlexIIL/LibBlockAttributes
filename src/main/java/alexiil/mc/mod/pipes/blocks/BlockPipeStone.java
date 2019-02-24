package alexiil.mc.mod.pipes.blocks;

import net.minecraft.world.BlockView;

public class BlockPipeStone extends BlockPipe {

    public BlockPipeStone(Settings settings) {
        super(settings);
    }

    @Override
    public TilePipe createBlockEntity(BlockView var1) {
        return new TilePipeStone();
    }
}
