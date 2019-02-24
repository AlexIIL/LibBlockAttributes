package alexiil.mc.mod.pipes.blocks;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.Direction;

public class TilePipeIron extends TilePipeSided {

    public TilePipeIron() {
        super(SimplePipeBlocks.IRON_PIPE_TILE, SimplePipeBlocks.IRON_PIPE);
    }

    @Override
    protected boolean canConnect(Direction dir, BlockEntity oTile) {
        return false;
    }

    @Override
    protected boolean canFaceDirection(Direction dir) {
        return isConnected(dir);
    }

    @Override
    protected List<EnumSet<Direction>> getOrderForItem(TravellingItem item, EnumSet<Direction> validDirections) {
        List<EnumSet<Direction>> order = super.getOrderForItem(item, validDirections);
        for (EnumSet<Direction> set : order) {
            set.remove(currentDirection());
        }
        return order;
    }

    @Override
    protected boolean canBounce() {
        return true;
    }
}
