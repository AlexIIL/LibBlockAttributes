package alexiil.mc.mod.pipes.blocks;

import net.minecraft.world.BlockView;

public class BlockPipeIron extends BlockPipeSided {

    public BlockPipeIron(Settings settings) {
        super(settings);
    }

    @Override
    public TilePipeSided createBlockEntity(BlockView view) {
        return new TilePipeIron();
    }
}
